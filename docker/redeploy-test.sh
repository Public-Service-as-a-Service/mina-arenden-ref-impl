#!/bin/bash
# Verifierar att en omdeploy av Compose-stacken inte tömmer databasen:
#   1. första deploy (tom volym), läs in en kundhändelse
#   2. omdeploy med ny build, nya containrar och bytt DB_PASSWORD (som i Dokploy)
#   3. kontrollera att kundhändelsen finns kvar och att db-prepare har tagit en säkerhetskopia
# Används i CI (.github/workflows/docker_ci.yml) och kan köras lokalt. Kräver docker compose, curl och openssl.
# Stacken och dess volymer tas bort när skriptet är klart.
set -euo pipefail

cd "$(dirname "$0")/.."
export COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-mina-arenden-redeploy-test}"
compose=(docker compose -f docker-compose.yml -f docker-compose.local.yml)
# Valfri extra compose-fil, t.ex. för att bygga appen på annat sätt i en miljö utan nätåtkomst från byggcontainern.
if [ -n "${COMPOSE_EXTRA_FILE:-}" ]; then
	compose+=(-f "${COMPOSE_EXTRA_FILE}")
fi

DB_PASSWORD="forsta-$(openssl rand -hex 12)"
DB_ROOT_PASSWORD="root-$(openssl rand -hex 12)"
API_KEYS_ADMIN="admin-$(openssl rand -hex 32)"
API_KEYS_FRAGA="fraga-$(openssl rand -hex 32)"
MINA_ARENDEN_SEED=false
export DB_PASSWORD DB_ROOT_PASSWORD API_KEYS_ADMIN API_KEYS_FRAGA MINA_ARENDEN_SEED

stada() {
	"${compose[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
}
trap stada EXIT
stada

vanta_pa_appen() {
	for _ in $(seq 1 90); do
		if curl -fsS http://localhost:8080/actuator/health/readiness >/dev/null 2>&1; then
			return 0
		fi
		sleep 2
	done
	"${compose[@]}" logs app db-prepare | tail -100
	echo "Tjänsten blev aldrig redo" >&2
	return 1
}

echo "== Första deploy"
"${compose[@]}" up -d --build
vanta_pa_appen

curl -fsS -X POST http://localhost:8080/2281/kundhandelser \
	-H "X-API-Key: ${API_KEYS_ADMIN}" -H 'Content-Type: application/json' \
	-d '[{"kundhandelseId":"REFKOM-REDEPLOY-1","part":{"arende":{"identifierare":"R-1","typ":"Diarienummer"}},
	      "rubrik":"Ska överleva omdeploy","beskrivning":"b","tidpunkt":"2026-09-01T10:00:00+02:00",
	      "kundhandelseTyp":"REFKOM.TEST.SKAPAD","producentarendetKraverKundatgard":false,"producentarendetKlart":false}]'
echo

echo "== Omdeploy med ny build och bytt DB_PASSWORD"
DB_PASSWORD="andra-$(openssl rand -hex 12)"
export DB_PASSWORD
"${compose[@]}" up -d --build --force-recreate
vanta_pa_appen

rubrik=$(curl -fsS http://localhost:8080/2281/kundhandelser/REFKOM-REDEPLOY-1 -H "X-API-Key: ${API_KEYS_ADMIN}" | grep -o '"rubrik" *: *"[^"]*"')
echo "Efter omdeploy: ${rubrik}"
[[ "${rubrik}" == *"Ska överleva omdeploy"* ]] || {
	echo "Kundhändelsen saknas efter omdeploy" >&2
	exit 1
}

# Frågegränssnittet med fråge-nyckel, och 401 utan nyckel.
curl -fsS -o /dev/null http://localhost:8080/2281/kundhandelseFragaSynkron -H "X-API-Key: ${API_KEYS_FRAGA}" \
	-H 'Content-Type: application/json' -H 'skv_client_correlation_id: redeploy-test' \
	-d '{"fraga":{"parter":[{"arende":{"identifierare":"R-1","typ":"Diarienummer"}}]},"anvandare":"199009090000"}'
status=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/2281/kundhandelser)
[ "${status}" = "401" ] || {
	echo "Förväntade 401 utan API-nyckel, fick ${status}" >&2
	exit 1
}

backuper=$("${compose[@]}" run --rm --no-deps --entrypoint sh db-prepare -c 'ls -1 /backups/*.sql.gz 2>/dev/null | wc -l')
echo "Säkerhetskopior: ${backuper}"
[ "${backuper}" -ge 1 ] || {
	echo "Ingen säkerhetskopia togs vid omdeploy" >&2
	exit 1
}

echo "OK: datat finns kvar efter omdeploy"
