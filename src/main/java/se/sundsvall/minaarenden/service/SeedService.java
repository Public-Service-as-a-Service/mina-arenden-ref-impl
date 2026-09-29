package se.sundsvall.minaarenden.service;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import se.sundsvall.minaarenden.api.model.NyKundhandelse;
import se.sundsvall.minaarenden.configuration.MinaArendenProperties;
import se.sundsvall.minaarenden.configuration.MinaArendenProperties.Producent;
import se.sundsvall.minaarenden.integration.db.KundhandelseRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loggar vid start hur många kundhändelser som finns per kommun, så att det efter en omdeploy syns direkt i loggen att
 * datat finns kvar. Om MINA_ARENDEN_SEED=true läses dessutom exempelhändelser in för de kommuner som inte har några
 * kundhändelser alls (endast utveckling/demo). Befintliga data rörs aldrig.
 */
@Component
public class SeedService implements ApplicationRunner {

	/** Prefix som exempelfilen är skriven med; byts mot kommunens prefix vid inläsning. */
	static final String EXEMPEL_PREFIX = "REFKOM";
	static final String EXEMPELFIL = "exempel-kundhandelser.json";

	private static final Logger LOG = LoggerFactory.getLogger(SeedService.class);

	private final MinaArendenProperties properties;
	private final KundhandelseService service;
	private final KundhandelseRepository repository;
	private final JsonMapper jsonMapper;

	public SeedService(final MinaArendenProperties properties, final KundhandelseService service, final KundhandelseRepository repository, final JsonMapper jsonMapper) {
		this.properties = properties;
		this.service = service;
		this.repository = repository;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void run(final ApplicationArguments args) throws IOException {
		for (final var producent : properties.producenter()) {
			final var antal = repository.countByMunicipalityId(producent.municipalityId());
			LOG.info("Ärendecachen innehåller {} kundhändelser för kommun {} ({})", antal, producent.municipalityId(), producent.namn());
			if (properties.seed() && antal == 0) {
				lasInExempel(producent);
			}
		}
	}

	private void lasInExempel(final Producent producent) throws IOException {
		try (var in = new ClassPathResource(EXEMPELFIL).getInputStream()) {
			final List<NyKundhandelse> exempel = jsonMapper.readValue(in, new TypeReference<>() {});
			exempel.forEach(ny -> bytPrefix(ny, producent.prefix()));
			final var antal = service.spara(producent.municipalityId(), exempel);
			LOG.info("Läste in {} exempelhändelser för kommun {} ({})", antal, producent.municipalityId(), producent.namn());
		}
	}

	/**
	 * Byter producentprefix i de fält där det förekommer (kundhandelseId och kundhandelseTyp), inget annat.
	 */
	static NyKundhandelse bytPrefix(final NyKundhandelse ny, final String prefix) {
		return ny
			.withKundhandelseId(bytInledning(ny.getKundhandelseId(), EXEMPEL_PREFIX + "-", prefix + "-"))
			.withKundhandelseTyp(bytInledning(ny.getKundhandelseTyp(), EXEMPEL_PREFIX + ".", prefix + "."));
	}

	private static String bytInledning(final String varde, final String fran, final String till) {
		return varde != null && varde.startsWith(fran) ? till + varde.substring(fran.length()) : varde;
	}
}
