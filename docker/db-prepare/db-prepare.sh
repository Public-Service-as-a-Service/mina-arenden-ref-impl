#!/bin/bash
# Körs som engångstjänst (db-prepare) vid varje deploy, efter att databasen är uppe och innan applikationen startar.
#
# 1. Säkerhetskopierar databasen till volymen mariadb-backups (om den innehåller tabeller), så att en omdeploy med
#    en ny version, inklusive nya Flyway-migreringar, alltid kan rullas tillbaka.
# 2. Rensar bort gamla säkerhetskopior så att högst DB_BACKUP_KEEP finns kvar.
# 3. Sätter lösenordet för applikationens databasanvändare till DB_PASSWORD. MariaDB-imagen läser MARIADB_PASSWORD
#    bara när volymen initieras första gången; utan det här steget skulle ett bytt DB_PASSWORD i Dokploy låsa ute
#    applikationen efter nästa deploy.
#
# Datat i databasen ändras aldrig av det här skriptet.
set -euo pipefail

: "${DB_HOST:=db}"
: "${DB_NAME:=minaarenden}"
: "${DB_USER:=minaarenden}"
: "${DB_BACKUP_DIR:=/backups}"
: "${DB_BACKUP_KEEP:=10}"
: "${DB_PASSWORD:?DB_PASSWORD måste vara satt}"
: "${DB_ROOT_PASSWORD:?DB_ROOT_PASSWORD måste vara satt}"

export MYSQL_PWD="${DB_ROOT_PASSWORD}"
klient=(mariadb --host="${DB_HOST}" --user=root --batch --skip-column-names)

for forsok in $(seq 1 30); do
	if "${klient[@]}" -e "SELECT 1" >/dev/null 2>&1; then
		break
	fi
	if [ "${forsok}" -eq 30 ]; then
		echo "db-prepare: kan inte ansluta till ${DB_HOST} som root. Har DB_ROOT_PASSWORD ändrats efter första deploy?" >&2
		exit 1
	fi
	sleep 2
done

antal_tabeller=$("${klient[@]}" -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${DB_NAME}'")
if [ "${antal_tabeller}" -gt 0 ]; then
	mkdir -p "${DB_BACKUP_DIR}"
	fil="${DB_BACKUP_DIR}/${DB_NAME}-$(date -u +%Y%m%dT%H%M%SZ).sql.gz"
	mariadb-dump --host="${DB_HOST}" --user=root --single-transaction --routines --triggers --databases "${DB_NAME}" | gzip > "${fil}.tmp"
	mv "${fil}.tmp" "${fil}"
	echo "db-prepare: säkerhetskopia sparad: ${fil} ($(du -h "${fil}" | cut -f1))"
	# Behåll de DB_BACKUP_KEEP senaste säkerhetskopiorna.
	ls -1t "${DB_BACKUP_DIR}/${DB_NAME}-"*.sql.gz 2>/dev/null | tail -n +"$((DB_BACKUP_KEEP + 1))" | xargs -r rm -f --
else
	echo "db-prepare: databasen ${DB_NAME} är tom (första deploy), ingen säkerhetskopia behövs"
fi

losenord=${DB_PASSWORD//\\/\\\\}
losenord=${losenord//\'/\\\'}
"${klient[@]}" -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${losenord}';
ALTER USER '${DB_USER}'@'%' IDENTIFIED BY '${losenord}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'%';"
echo "db-prepare: databasanvändaren ${DB_USER} är uppdaterad"
