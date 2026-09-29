package se.sundsvall.minaarenden.service.mapper;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import se.sundsvall.minaarenden.api.model.Kund;
import se.sundsvall.minaarenden.api.model.Part;
import se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.NullNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static se.sundsvall.minaarenden.service.TestdataFactory.MUNICIPALITY_ID;
import static se.sundsvall.minaarenden.service.TestdataFactory.PRODUCENT;
import static se.sundsvall.minaarenden.service.TestdataFactory.entity;
import static se.sundsvall.minaarenden.service.TestdataFactory.nyKundhandelse;

class KundhandelseMapperTest {

	@Test
	void toEntityFyllerIStandardvarden() {
		final var ny = nyKundhandelse("TESTKOP-1").withTidpunkt("2026-09-15 12:00:00");
		ny.getPart().getKund().withTillagg(" ");

		final var entity = KundhandelseMapper.toEntity(MUNICIPALITY_ID, PRODUCENT, "6.1", ny, KundhandelseEntity.create());

		assertThat(entity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(entity.getKundhandelseId()).isEqualTo("TESTKOP-1");
		assertThat(entity.getProducent()).isEqualTo("Testköpings kommun");
		assertThat(entity.getKundIdentifierare()).isEqualTo("199009090000");
		assertThat(entity.getKundTyp()).isEqualTo("Personnummer");
		assertThat(entity.getKundTillagg()).isNull();
		assertThat(entity.getArendeIdentifierare()).isEqualTo("BYGG-1");
		assertThat(entity.getArendeTyp()).isEqualTo("Diarienummer");
		assertThat(entity.getSprak()).isEqualTo("sv");
		assertThat(entity.getVersion()).isEqualTo("6.1");
		assertThat(entity.getTidpunkt()).isEqualTo(Instant.parse("2026-09-15T10:00:00Z"));
		assertThat(entity.getReferenser()).isEqualTo("{\"diarienummer\":\"BYGG-1\"}");
		assertThat(entity.getUtokadInformation()).isNull();
		assertThat(entity.getTaggar()).containsExactly("bygglov");
	}

	@Test
	void toEntityErsatterPartOchBehallerAngivnaVarden() {
		final var ny = nyKundhandelse("TESTKOP-1")
			.withPart(Part.create().withKund(Kund.create().withIdentifierare("165560001234").withTyp("Organisationsnummer").withTillagg("2")))
			.withSprak("en")
			.withVersion("6.0")
			.withTaggar(null);

		final var entity = KundhandelseMapper.toEntity(MUNICIPALITY_ID, PRODUCENT, "6.1", ny, entity("TESTKOP-1"));

		assertThat(entity.getKundTillagg()).isEqualTo("2");
		assertThat(entity.getArendeIdentifierare()).isNull();
		assertThat(entity.getArendeTyp()).isNull();
		assertThat(entity.getSprak()).isEqualTo("en");
		assertThat(entity.getVersion()).isEqualTo("6.0");
		assertThat(entity.getTaggar()).isEmpty();

		ny.withPart(Part.create().withArende(se.sundsvall.minaarenden.api.model.Arende.create().withIdentifierare("A").withTyp("Diarienummer")));
		KundhandelseMapper.toEntity(MUNICIPALITY_ID, PRODUCENT, "6.1", ny, entity);
		assertThat(entity.getKundIdentifierare()).isNull();
		assertThat(entity.getKundTyp()).isNull();
		assertThat(entity.getKundTillagg()).isNull();
	}

	@Test
	void toKundhandelseOchPart() {
		final var entity = entity("TESTKOP-1").withUtokadInformation("{\"senastDatum\":\"2026-09-30\"}");

		final var kundhandelse = KundhandelseMapper.toKundhandelse(entity);
		final var part = KundhandelseMapper.toPart(entity);

		assertThat(kundhandelse.getTidpunkt()).isEqualTo("2026-09-20T10:00:00+02:00");
		assertThat(kundhandelse.getUtokadInformation().get("senastDatum").asString()).isEqualTo("2026-09-30");
		assertThat(kundhandelse.getReferenser().get("diarienummer").asString()).isEqualTo("BYGG-1");
		assertThat(kundhandelse.isProducentarendetKlart()).isTrue();
		assertThat(kundhandelse.isProducentarendetKraverKundatgard()).isFalse();
		assertThat(part.getKund().getIdentifierare()).isEqualTo("199009090000");
		assertThat(part.getArende().getIdentifierare()).isEqualTo("BYGG-1");

		final var utanPart = KundhandelseMapper.toPart(entity.withKundIdentifierare(null).withArendeIdentifierare(null));
		assertThat(utanPart.isEmpty()).isTrue();
	}

	@Test
	void toCachadKundhandelse() {
		final var cachad = KundhandelseMapper.toCachadKundhandelse(entity("TESTKOP-1"));

		assertThat(cachad.getKundhandelse().getKundhandelseId()).isEqualTo("TESTKOP-1");
		assertThat(cachad.getPart().getKund().getTyp()).isEqualTo("Personnummer");
		assertThat(cachad.getTaggar()).containsExactly("bygglov");
	}

	@Test
	void json() {
		assertThat(KundhandelseMapper.skrivJson(null)).isNull();
		assertThat(KundhandelseMapper.skrivJson(NullNode.getInstance())).isNull();
		assertThat(KundhandelseMapper.skrivJson(MissingNode.getInstance())).isNull();
		assertThat(KundhandelseMapper.skrivJson(JsonNodeFactory.instance.objectNode().put("a", 1))).isEqualTo("{\"a\":1}");
		assertThat(KundhandelseMapper.lasJson(null)).isNull();
		assertThatIllegalStateException().isThrownBy(() -> KundhandelseMapper.lasJson("{inte json"));
	}
}
