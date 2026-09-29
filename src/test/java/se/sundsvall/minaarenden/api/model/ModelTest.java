package se.sundsvall.minaarenden.api.model;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSettersExcluding;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Getters, setters, equals, hashCode och toString för API-modellerna.
 */
class ModelTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> JsonNodeFactory.instance.objectNode().put("id", System.nanoTime()), JsonNode.class);
	}

	static Stream<Class<?>> modeller() {
		return Stream.of(Arende.class, Behandling.class, CachadKundhandelse.class, Delfraga.class, ErrorResponse.class, Fraga.class, Kund.class, Kundhandelse.class,
			KundhandelseFragaRequest.class, KundhandelseFragaResponse.class, KundhandelserForPart.class, Metadata.class, NyKundhandelse.class, Paginering.class, Sortering.class);
	}

	@ParameterizedTest
	@MethodSource("modeller")
	void testBean(final Class<?> modell) {
		assertThat(modell, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testPart() {
		assertThat(Part.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSettersExcluding("empty"),
			hasValidBeanHashCodeExcluding("empty"),
			hasValidBeanEqualsExcluding("empty"),
			hasValidBeanToStringExcluding("empty")));
	}

	@Test
	void personnummerMaskerasIToString() {
		final var kund = Kund.create().withIdentifierare("199009090000").withTyp("Personnummer");
		final var request = KundhandelseFragaRequest.create().withAnvandare("194903012658").withFraga(Fraga.create());

		assertThat(kund.toString()).doesNotContain("199009090000").contains("typ=Personnummer");
		assertThat(request.toString()).doesNotContain("194903012658");
	}

	@Test
	void tomPart() {
		assertThat(Part.create().isEmpty()).isTrue();
		assertThat(Part.create().withKund(Kund.create()).isEmpty()).isFalse();
		assertThat(Part.create().withArende(Arende.create()).isEmpty()).isFalse();
	}

	@Test
	void builderMetoder() {
		final var paginering = Paginering.create().withOffset(1).withLimit(2);
		final var sortering = Sortering.create().withAttribut("RUBRIK").withStigande(true);

		assertThat(paginering.getOffset()).isEqualTo(1);
		assertThat(paginering.getLimit()).isEqualTo(2);
		assertThat(sortering.getAttribut()).isEqualTo("RUBRIK");
		assertThat(sortering.getStigande()).isTrue();
		assertThat(new AntalKundhandelser(3).antal()).isEqualTo(3);
	}
}
