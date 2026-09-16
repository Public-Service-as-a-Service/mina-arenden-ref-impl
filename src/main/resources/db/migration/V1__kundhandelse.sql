-- Ärendecache: kundhändelser enligt Mina ärendens standard, med den part
-- (kund och/eller ärende) som händelsen gäller. Kolumnnamnen följer API-specifikationen.

CREATE TABLE kundhandelse (
    id                                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    kundhandelse_id                     VARCHAR(100)  NOT NULL,
    producent                           VARCHAR(200)  NOT NULL,
    kund_identifierare                  VARCHAR(12),
    kund_typ                            VARCHAR(20),
    kund_tillagg                        VARCHAR(20),
    arende_identifierare                VARCHAR(100),
    arende_typ                          VARCHAR(30),
    rubrik                              VARCHAR(255)  NOT NULL,
    beskrivning                         TEXT          NOT NULL,
    sprak                               VARCHAR(10)   NOT NULL,
    tidpunkt                            DATETIME(3)   NOT NULL,
    kundhandelse_typ                    VARCHAR(200)  NOT NULL,
    producentarendet_kraver_kundatgard  BOOLEAN       NOT NULL,
    producentarendet_klart              BOOLEAN       NOT NULL,
    version                             VARCHAR(10)   NOT NULL,
    utokad_information                  TEXT,
    referenser                          TEXT,
    skapad                              DATETIME(3)   NOT NULL,
    CONSTRAINT uq_kundhandelse_id UNIQUE (kundhandelse_id)
);

CREATE INDEX ix_kundhandelse_kund    ON kundhandelse (kund_identifierare, kund_typ);
CREATE INDEX ix_kundhandelse_arende  ON kundhandelse (arende_identifierare, arende_typ);
CREATE INDEX ix_kundhandelse_tidpunkt ON kundhandelse (tidpunkt);
CREATE INDEX ix_kundhandelse_typ     ON kundhandelse (kundhandelse_typ);

-- Taggar som pekar ut producentärendetyper eller kundhändelsetyper (fraga.taggar).
CREATE TABLE kundhandelse_tagg (
    kundhandelse_ref  BIGINT       NOT NULL,
    tagg              VARCHAR(100) NOT NULL,
    PRIMARY KEY (kundhandelse_ref, tagg),
    CONSTRAINT fk_tagg_kundhandelse FOREIGN KEY (kundhandelse_ref) REFERENCES kundhandelse (id) ON DELETE CASCADE
);

CREATE INDEX ix_kundhandelse_tagg ON kundhandelse_tagg (tagg);
