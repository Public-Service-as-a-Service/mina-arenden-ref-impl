# Mina ärenden – referensimplementation

Enklast möjliga producent enligt [Mina ärendens standard för samlad ärendeåterkoppling](https://www.digg.se/digitala-tjanster/mina-arenden---arendeaterkoppling-fran-det-offentliga),
byggd i Java med en ärendecache i MariaDB. Tänkt som utgångspunkt för en kommun som vill
lämna kundhändelser till Mina ärenden, eller som vill pröva standarden lokalt innan anslutning.

```
Verksamhetssystem ──POST /kundhandelser──▶ Ärendecache (MariaDB)
                                                 │
Vidareförmedlingstjänst (Skatteverket) ──POST /kundhandelseFragaSynkron──▶ svar med kundhändelser
```

- **Ärendecache:** kundhändelser lagras i tabellen `kundhandelse` med kolumner som motsvarar
  fälten i API-specifikationen (rubrik, beskrivning, sprak, tidpunkt, kundhandelseTyp,
  producentarendetKraverKundatgard, producentarendetKlart, version, utokadInformation,
  referenser) samt den part (kund och/eller ärende) händelsen gäller. Taggar i en egen tabell.
- **Frågegränssnitt:** `POST /kundhandelseFragaSynkron` med samma request och response som
  Skatteverkets API *Mina ärenden kundhändelser* (RAML `mina-arenden-kundhandelser` 1.0.0,
  `vidareformedlingstjanst-fraga-library` 1.0.8). Stödjer parter (kund, ärende eller båda),
  kundhändelsetyper med prefixmatchning, taggar, datumintervall, sortering och paginering.
- **Inläsning:** `POST /kundhandelser` tar emot en lista med kundhändelser; samma
  `kundhandelseId` ersätter befintlig post.
- **Dokumentation:** Swagger UI på `/swagger-ui.html`, OpenAPI på `/v3/api-docs`.
- **Drift:** container med hälsokontroll på `/actuator/health`; `docker-compose.yml` med MariaDB.

Teknik: Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, MariaDB (H2 i tester), springdoc-openapi.

## Köra lokalt

Med Docker Compose (bygger applikationen och startar MariaDB):

```bash
cp .env.example .env          # sätt DB_PASSWORD
docker compose up --build
# Swagger UI: http://localhost:8080/swagger-ui.html
```

Utan Docker, mot en egen MariaDB:

```bash
export DB_URL=jdbc:mariadb://localhost:3306/minaarenden DB_USER=minaarenden DB_PASSWORD=hemligt
export MINA_ARENDEN_SEED=true
./gradlew bootRun
```

Tester (H2 i MariaDB-läge, ingen databas behövs):

```bash
./gradlew test
```

Med `MINA_ARENDEN_SEED=true` läses fem exempelhändelser in vid första start
(`src/main/resources/exempel-kundhandelser.json`), så att frågor kan provas direkt.

## API

### Fråga om kundhändelser

```bash
curl -s http://localhost:8080/kundhandelseFragaSynkron \
  -H 'Content-Type: application/json' \
  -H 'skv_client_correlation_id: 0002aa29-49f2-4baf-be51-c7c39c9824b4' \
  -H 'Accept-Language: sv' \
  -d '{
    "fraga": {
      "parter": [{ "kund": { "identifierare": "199009090000", "typ": "Personnummer" } }],
      "kundhandelseTyper": ["REFKOM.BYGGLOV"],
      "startDatum": "2026-08-01",
      "slutDatum": "2026-09-30",
      "behandling": { "sortering": [{ "attribut": "TIDPUNKT", "stigande": false }], "paginering": { "offset": 0, "limit": 20 } }
    },
    "anvandare": "199009090000"
  }'
```

Svaret följer specifikationen: `kundhandelser` per part med `totaltAntalKundhandelser` och
`kundhandelserForPart`, `metadata` som ekar frågan, och `delfragor` med en rad per part
(`producent`, `status`, `httpCode`). Som ensam producent svarar tjänsten alltid `200`;
vidareförmedlingstjänsten sätter `206` när någon producent fallerar.

Regler som implementeras:

| Fält | Beteende |
|---|---|
| `parter[].kund` + `arende` | Båda måste stämma om båda anges |
| `kundhandelseTyper` | `REFKOM.BYGGLOV` matchar `REFKOM.BYGGLOV` och allt under (`REFKOM.BYGGLOV.*`) |
| `taggar` | Minst en tagg ska finnas på händelsen |
| `startDatum`, `slutDatum` | Inklusive, kl. 00:00 till 23:59:59.999 svensk tid |
| `behandling.sortering` | Flera attribut i prioritetsordning; utan sortering nyast först |
| `behandling.paginering` | Kräver sortering; `totaltAntalKundhandelser` anger antal före paginering |
| Fel | `{"message": "..."}` med 400, 401, 404, 405, 415 eller 500 |

Obligatorisk header: `skv_client_correlation_id`. Om `CLIENT_ID` och `CLIENT_SECRET` är satta
krävs även headrarna `client_id` och `client_secret` (enkel ersättning för OAuth 2 i test).

### Läsa in kundhändelser i cachen

```bash
curl -s http://localhost:8080/kundhandelser -H 'Content-Type: application/json' -d '[{
  "kundhandelseId": "REFKOM-BYGG-2026-00123-3",
  "part": { "kund": { "identifierare": "199009090000", "typ": "Personnummer" },
            "arende": { "identifierare": "BYGG-2026-00123", "typ": "Diarienummer" } },
  "rubrik": "Beslut om bygglov",
  "beskrivning": "Bygglov har beviljats. Beslutet finns på Mina sidor.",
  "tidpunkt": "2026-09-20T10:00:00+02:00",
  "kundhandelseTyp": "REFKOM.BYGGLOV.BESLUT",
  "producentarendetKraverKundatgard": false,
  "producentarendetKlart": true,
  "taggar": ["bygglov"],
  "referenser": { "diarienummer": "BYGG-2026-00123" }
}]'
```

`producent`, `sprak` (`sv`) och `version` fylls i från konfigurationen om de utelämnas.
`tidpunkt` tas emot som RFC 3339 eller lokal tid `ÅÅÅÅ-MM-DD HH:MM:SS` och lämnas alltid ut som
RFC 3339 med svensk tidszon. Även `GET /kundhandelser/{id}`, `DELETE /kundhandelser/{id}` och
`GET /kundhandelser` (antal). Skyddas med headern `X-Api-Key` om `ADMIN_API_KEY` är satt.

## Datamodell

```
kundhandelse
  id, kundhandelse_id (unik), producent,
  kund_identifierare, kund_typ, kund_tillagg,          -- part.kund
  arende_identifierare, arende_typ,                     -- part.arende
  rubrik, beskrivning, sprak, tidpunkt, kundhandelse_typ,
  producentarendet_kraver_kundatgard, producentarendet_klart, version,
  utokad_information (JSON-text), referenser (JSON-text), skapad
kundhandelse_tagg
  kundhandelse_ref → kundhandelse.id, tagg
```

Schemat skapas av Flyway (`src/main/resources/db/migration`). Ändra modellen genom att lägga
till en ny migration, inte genom att ändra `V1`.

## Konfiguration

| Variabel | Standard | Beskrivning |
|---|---|---|
| `DB_URL` | `jdbc:mariadb://localhost:3306/minaarenden` | JDBC-URL till MariaDB |
| `DB_USER`, `DB_PASSWORD` | `minaarenden` / `minaarenden` | Databasanvändare |
| `MINA_ARENDEN_PRODUCENT` | `Referenskommunen` | Producentens namn i svaren |
| `MINA_ARENDEN_PREFIX` | `REFKOM` | Producentprefix i kundhändelsetyper |
| `MINA_ARENDEN_STANDARD_VERSION` | `6.1` | Standardversion som anges i `version` |
| `MINA_ARENDEN_SEED` | `false` | Läs in exempelhändelser om cachen är tom |
| `CLIENT_ID`, `CLIENT_SECRET` | tomma | Krävs som headrar på frågan om satta |
| `ADMIN_API_KEY` | tom | Krävs som `X-Api-Key` på `/kundhandelser` om satt |
| `PORT` | `8080` | Lyssningsport |

## Deploy i Dokploy

Enklast som **Compose-tjänst**, då följer MariaDB med:

1. **Create Service → Compose**. Provider GitHub, repot `Public-Service-as-a-Service/mina-arenden-ref-impl`,
   branch `main`, Compose Path `docker-compose.yml`.
2. Fliken **Environment**: sätt minst `DB_PASSWORD` (t.ex. `openssl rand -hex 16`). Sätt gärna
   `MINA_ARENDEN_PRODUCENT`, `MINA_ARENDEN_PREFIX`, `CLIENT_ID`, `CLIENT_SECRET` och `ADMIN_API_KEY`.
3. Fliken **Domains**: lägg till domän för tjänsten `app`, container port `8080`, HTTPS på.
4. **Deploy**. Databasen ligger i volymen `mariadb-data` och överlever redeploy.

Som fristående **Application** (Build Type Dockerfile) fungerar det också; peka då `DB_URL`
mot en MariaDB-tjänst i Dokploy och sätt `DB_USER`/`DB_PASSWORD`. Hälsokontroll:
`/actuator/health` (readiness på `/actuator/health/readiness`).

## Avgränsningar

- Ingen OAuth 2. Skatteverkets vidareförmedlingstjänst autentiserar sig mot producenter enligt
  anslutningsvillkoren; referensimplementationen har en enkel header-kontroll som platshållare.
- Sortering och paginering görs i minnet per part efter databasfrågan, vilket räcker för en
  kommuns volymer men bör flyttas till SQL om cachen blir stor.
- Auktorisation av slutanvändaren (`anvandare`) görs inte; producenten ansvarar för detta i
  skarp drift.
- Endast den synkrona frågan implementeras. Standardens övriga delar (till exempel
  kundhändelsetypernas katalog och verksamhetsguiden) finns på
  [dataportal.se](https://www.dataportal.se/specifications/minaarenden/6.1).

## Källor

- [Digg: Mina ärenden](https://www.digg.se/digitala-tjanster/mina-arenden---arendeaterkoppling-fran-det-offentliga)
- [Skatteverkets utvecklarportal: API Mina ärenden kundhändelser](https://www7.skatteverket.se/portal/apier-och-oppna-data/utvecklarportalen/api/mina-arenden-kundhandelser/1.0.0/%C3%96versikt)
- [API-definition (RAML, zip)](https://www7.skatteverket.se/portal-wapi/open/apier-och-oppna-data/utvecklarportalen/v1/v2/getRamlZip/mina-arenden-kundhandelser/1.0.0)
- [Tjänstebeskrivning (pdf)](https://www7.skatteverket.se/portal-wapi/open/apier-och-oppna-data/utvecklarportalen/v1/getFile/tjanstebeskrivning-mina-arenden-kundhandelser-v1)

## Licens

MIT, se `LICENSE`.
