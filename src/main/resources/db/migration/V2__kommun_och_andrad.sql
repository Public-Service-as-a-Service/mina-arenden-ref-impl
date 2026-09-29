-- Kundhändelser knyts till kommun (dept44-konventionen /{municipalityId}/...), så att en instans kan vara producent
-- för flera kommuner. Befintliga rader, t.ex. från en tidigare deploy, får kommun-id från konfigurationen
-- (MINA_ARENDEN_MUNICIPALITY_ID, platshållaren default_municipality_id) och finns kvar oförändrade i övrigt.
ALTER TABLE kundhandelse
    ADD COLUMN municipality_id VARCHAR(4) NOT NULL DEFAULT '${default_municipality_id}' AFTER id;

ALTER TABLE kundhandelse
    ALTER COLUMN municipality_id DROP DEFAULT;

-- Tidpunkt för senaste ersättning via POST /{municipalityId}/kundhandelser.
ALTER TABLE kundhandelse
    ADD COLUMN andrad DATETIME(3) NULL AFTER skapad;

-- kundhandelseId är unikt per kommun i stället för globalt.
ALTER TABLE kundhandelse
    ADD CONSTRAINT uq_kundhandelse_municipality_id_kundhandelse_id UNIQUE (municipality_id, kundhandelse_id);

ALTER TABLE kundhandelse
    DROP INDEX uq_kundhandelse_id;

-- Frågor filtrerar alltid på kommun först.
DROP INDEX ix_kundhandelse_kund ON kundhandelse;

CREATE INDEX ix_kundhandelse_kund ON kundhandelse (municipality_id, kund_identifierare, kund_typ);

DROP INDEX ix_kundhandelse_arende ON kundhandelse;

CREATE INDEX ix_kundhandelse_arende ON kundhandelse (municipality_id, arende_identifierare, arende_typ);
