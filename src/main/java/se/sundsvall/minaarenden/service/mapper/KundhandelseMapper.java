package se.sundsvall.minaarenden.service.mapper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import se.sundsvall.minaarenden.api.model.Arende;
import se.sundsvall.minaarenden.api.model.CachadKundhandelse;
import se.sundsvall.minaarenden.api.model.Kund;
import se.sundsvall.minaarenden.api.model.Kundhandelse;
import se.sundsvall.minaarenden.api.model.NyKundhandelse;
import se.sundsvall.minaarenden.api.model.Part;
import se.sundsvall.minaarenden.configuration.MinaArendenProperties.Producent;
import se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity;
import se.sundsvall.minaarenden.service.Tidpunkt;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Mappning mellan API-modellen och ärendecachen. utokadInformation och referenser lagras som JSON-text.
 */
public final class KundhandelseMapper {

	static final String STANDARD_SPRAK = "sv";

	private static final JsonMapper JSON_MAPPER = JsonMapper.shared();

	private KundhandelseMapper() {}

	public static Kundhandelse toKundhandelse(final KundhandelseEntity entity) {
		return Kundhandelse.create()
			.withKundhandelseId(entity.getKundhandelseId())
			.withRubrik(entity.getRubrik())
			.withBeskrivning(entity.getBeskrivning())
			.withSprak(entity.getSprak())
			.withProducent(entity.getProducent())
			.withTidpunkt(Tidpunkt.formatera(entity.getTidpunkt()))
			.withKundhandelseTyp(entity.getKundhandelseTyp())
			.withProducentarendetKraverKundatgard(entity.isProducentarendetKraverKundatgard())
			.withProducentarendetKlart(entity.isProducentarendetKlart())
			.withVersion(entity.getVersion())
			.withUtokadInformation(lasJson(entity.getUtokadInformation()))
			.withReferenser(lasJson(entity.getReferenser()));
	}

	public static Part toPart(final KundhandelseEntity entity) {
		final var kund = Optional.ofNullable(entity.getKundIdentifierare())
			.map(identifierare -> Kund.create().withIdentifierare(identifierare).withTyp(entity.getKundTyp()).withTillagg(entity.getKundTillagg()))
			.orElse(null);
		final var arende = Optional.ofNullable(entity.getArendeIdentifierare())
			.map(identifierare -> Arende.create().withIdentifierare(identifierare).withTyp(entity.getArendeTyp()))
			.orElse(null);
		return Part.create().withKund(kund).withArende(arende);
	}

	public static CachadKundhandelse toCachadKundhandelse(final KundhandelseEntity entity) {
		return CachadKundhandelse.create()
			.withPart(toPart(entity))
			.withKundhandelse(toKundhandelse(entity))
			.withTaggar(List.copyOf(entity.getTaggar()));
	}

	/**
	 * Fyller i en ny eller befintlig entitet från en inläst kundhändelse. producent, sprak och version sätts från
	 * konfigurationen om de saknas.
	 *
	 * @param  municipalityId  kommun
	 * @param  producent       producenten för kommunen
	 * @param  standardVersion version av standarden som används om kundhändelsen inte anger någon
	 * @param  ny              inläst kundhändelse
	 * @param  entity          entitet att fylla i
	 * @return                 entiteten
	 */
	public static KundhandelseEntity toEntity(final String municipalityId, final Producent producent, final String standardVersion, final NyKundhandelse ny,
		final KundhandelseEntity entity) {

		final var kund = ny.getPart().getKund();
		final var arende = ny.getPart().getArende();
		return entity
			.withMunicipalityId(municipalityId)
			.withKundhandelseId(ny.getKundhandelseId())
			.withProducent(producent.namn())
			.withKundIdentifierare(kund == null ? null : kund.getIdentifierare())
			.withKundTyp(kund == null ? null : kund.getTyp())
			.withKundTillagg(kund == null ? null : blankTillNull(kund.getTillagg()))
			.withArendeIdentifierare(arende == null ? null : arende.getIdentifierare())
			.withArendeTyp(arende == null ? null : arende.getTyp())
			.withRubrik(ny.getRubrik())
			.withBeskrivning(ny.getBeskrivning())
			.withSprak(Optional.ofNullable(blankTillNull(ny.getSprak())).orElse(STANDARD_SPRAK))
			.withTidpunkt(Tidpunkt.tolka(ny.getTidpunkt()))
			.withKundhandelseTyp(ny.getKundhandelseTyp())
			.withProducentarendetKraverKundatgard(ny.getProducentarendetKraverKundatgard())
			.withProducentarendetKlart(ny.getProducentarendetKlart())
			.withVersion(Optional.ofNullable(blankTillNull(ny.getVersion())).orElse(standardVersion))
			.withUtokadInformation(skrivJson(ny.getUtokadInformation()))
			.withReferenser(skrivJson(ny.getReferenser()))
			.withTaggar(ny.getTaggar() == null ? null : new LinkedHashSet<>(ny.getTaggar()));
	}

	static JsonNode lasJson(final String json) {
		if (json == null) {
			return null;
		}
		try {
			return JSON_MAPPER.readTree(json);
		} catch (final JacksonException e) {
			throw new IllegalStateException("Ogiltig JSON i databasen", e);
		}
	}

	static String skrivJson(final JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) {
			return null;
		}
		return JSON_MAPPER.writeValueAsString(node);
	}

	private static String blankTillNull(final String varde) {
		return varde == null || varde.isBlank() ? null : varde;
	}
}
