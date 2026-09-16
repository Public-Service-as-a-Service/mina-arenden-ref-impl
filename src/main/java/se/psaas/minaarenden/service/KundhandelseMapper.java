package se.psaas.minaarenden.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.stereotype.Component;
import se.psaas.minaarenden.api.dto.Arende;
import se.psaas.minaarenden.api.dto.Kund;
import se.psaas.minaarenden.api.dto.Kundhandelse;
import se.psaas.minaarenden.api.dto.NyKundhandelse;
import se.psaas.minaarenden.api.dto.Part;
import se.psaas.minaarenden.config.MinaArendenProperties;
import se.psaas.minaarenden.domain.KundhandelseEntity;

@Component
public class KundhandelseMapper {

    private final ObjectMapper objectMapper;
    private final MinaArendenProperties properties;

    public KundhandelseMapper(ObjectMapper objectMapper, MinaArendenProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public Kundhandelse toDto(KundhandelseEntity e) {
        return new Kundhandelse(
                e.getKundhandelseId(),
                e.getRubrik(),
                e.getBeskrivning(),
                e.getSprak(),
                e.getProducent(),
                Tidpunkt.format(e.getTidpunkt()),
                e.getKundhandelseTyp(),
                e.isProducentarendetKraverKundatgard(),
                e.isProducentarendetKlart(),
                e.getVersion(),
                readJson(e.getUtokadInformation()),
                readJson(e.getReferenser()));
    }

    public Part partOf(KundhandelseEntity e) {
        Kund kund = e.getKundIdentifierare() == null ? null : new Kund(e.getKundIdentifierare(), e.getKundTillagg(), e.getKundTyp());
        Arende arende = e.getArendeIdentifierare() == null ? null : new Arende(e.getArendeIdentifierare(), e.getArendeTyp());
        return new Part(kund, arende);
    }

    public KundhandelseEntity toEntity(NyKundhandelse n, KundhandelseEntity target) {
        target.setKundhandelseId(n.kundhandelseId());
        target.setProducent(properties.producent());
        if (n.part().kund() != null) {
            target.setKundIdentifierare(n.part().kund().identifierare());
            target.setKundTyp(n.part().kund().typ());
            target.setKundTillagg(blankToNull(n.part().kund().tillagg()));
        } else {
            target.setKundIdentifierare(null);
            target.setKundTyp(null);
            target.setKundTillagg(null);
        }
        if (n.part().arende() != null) {
            target.setArendeIdentifierare(n.part().arende().identifierare());
            target.setArendeTyp(n.part().arende().typ());
        } else {
            target.setArendeIdentifierare(null);
            target.setArendeTyp(null);
        }
        target.setRubrik(n.rubrik());
        target.setBeskrivning(n.beskrivning());
        target.setSprak(n.sprak() == null || n.sprak().isBlank() ? "sv" : n.sprak());
        target.setTidpunkt(Tidpunkt.parse(n.tidpunkt()));
        target.setKundhandelseTyp(n.kundhandelseTyp());
        target.setProducentarendetKraverKundatgard(n.producentarendetKraverKundatgard());
        target.setProducentarendetKlart(n.producentarendetKlart());
        target.setVersion(n.version() == null || n.version().isBlank() ? properties.standardVersion() : n.version());
        target.setUtokadInformation(writeJson(n.utokadInformation()));
        target.setReferenser(writeJson(n.referenser()));
        target.setTaggar(n.taggar() == null ? null : new java.util.LinkedHashSet<>(n.taggar()));
        if (target.getSkapad() == null) {
            target.setSkapad(Instant.now());
        }
        return target;
    }

    private JsonNode readJson(String s) {
        if (s == null) {
            return null;
        }
        try {
            return objectMapper.readTree(s);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Ogiltig JSON i databasen", e);
        }
    }

    private String writeJson(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(n);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Ogiltig JSON", e);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
