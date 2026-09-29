package se.sundsvall.minaarenden.configuration;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.minaarenden.Application;
import se.sundsvall.minaarenden.configuration.MinaArendenProperties.Producent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class MinaArendenPropertiesTest {

	@Autowired
	private MinaArendenProperties properties;

	@Test
	void lasesFranKonfigurationen() {
		assertThat(properties.standardVersion()).isEqualTo("6.1");
		assertThat(properties.seed()).isFalse();
		assertThat(properties.producenter()).containsExactly(
			new Producent("2281", "Testköpings kommun", "TESTKOP"),
			new Producent("2262", "Timrå kommun", "TIMRA"));
	}

	@Test
	void producentPerKommun() {
		assertThat(properties.producent("2262")).hasValue(new Producent("2262", "Timrå kommun", "TIMRA"));
		assertThat(properties.producent("1480")).isEmpty();
	}

	@Test
	void sammaKommunFarInteFinnasTvaGanger() {
		final var producenter = List.of(new Producent("2281", "A", "A"), new Producent("2281", "B", "B"));

		assertThatIllegalStateException()
			.isThrownBy(() -> new MinaArendenProperties("6.1", false, producenter))
			.withMessageContaining("2281");
	}

	@Test
	void utanProducenterBlirListanTom() {
		assertThat(new MinaArendenProperties("6.1", false, null).producenter()).isEmpty();
	}
}
