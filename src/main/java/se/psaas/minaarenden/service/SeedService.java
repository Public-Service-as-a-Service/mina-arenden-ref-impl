package se.psaas.minaarenden.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import se.psaas.minaarenden.api.dto.NyKundhandelse;
import se.psaas.minaarenden.config.MinaArendenProperties;

/**
 * Läser in exempelhändelser vid start om MINA_ARENDEN_SEED=true och cachen är tom. Avsett för
 * utveckling och demonstration; profilen production stänger av det.
 */
@Component
public class SeedService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);

    /** Prefix som exempelfilen är skriven med; byts mot konfigurerat prefix vid inläsning. */
    static final String EXEMPEL_PREFIX = "REFKOM";

    private final MinaArendenProperties properties;
    private final KundhandelseService service;
    private final ObjectMapper objectMapper;

    public SeedService(MinaArendenProperties properties, KundhandelseService service, ObjectMapper objectMapper) {
        this.properties = properties;
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!properties.seed()) {
            return;
        }
        if (service.antal() > 0) {
            log.info("Ärendecachen innehåller redan {} kundhändelser, hoppar över exempeldata", service.antal());
            return;
        }
        try (InputStream in = new ClassPathResource("exempel-kundhandelser.json").getInputStream()) {
            List<NyKundhandelse> exempel = objectMapper.readValue(in, new TypeReference<>() {});
            List<NyKundhandelse> nya = exempel.stream().map(n -> medPrefix(n, properties.prefix())).toList();
            int antal = service.spara(nya);
            log.info("Läste in {} exempelhändelser för producenten {}", antal, properties.producent());
        }
    }

    /** Byter producentprefix i de fält där det förekommer (kundhandelseId och kundhandelseTyp), inget annat. */
    public static NyKundhandelse medPrefix(NyKundhandelse n, String prefix) {
        return new NyKundhandelse(
                bytPrefix(n.kundhandelseId(), EXEMPEL_PREFIX + "-", prefix + "-"),
                n.part(),
                n.rubrik(),
                n.beskrivning(),
                n.sprak(),
                n.tidpunkt(),
                bytPrefix(n.kundhandelseTyp(), EXEMPEL_PREFIX + ".", prefix + "."),
                n.producentarendetKraverKundatgard(),
                n.producentarendetKlart(),
                n.version(),
                n.utokadInformation(),
                n.referenser(),
                n.taggar());
    }

    private static String bytPrefix(String varde, String fran, String till) {
        return varde != null && varde.startsWith(fran) ? till + varde.substring(fran.length()) : varde;
    }
}
