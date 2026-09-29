package se.sundsvall.minaarenden.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.minaarenden.configuration.MinaArendenProperties;
import se.sundsvall.minaarenden.configuration.MinaArendenProperties.Producent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class ProducentServiceTest {

	private static final Producent PRODUCENT = new Producent("2281", "Testköpings kommun", "TESTKOP");

	private final ProducentService service = new ProducentService(new MinaArendenProperties("6.2", false, List.of(PRODUCENT)));

	@Test
	void hamta() {
		assertThat(service.hamta("2281")).isEqualTo(PRODUCENT);
		assertThat(service.standardVersion()).isEqualTo("6.2");
	}

	@Test
	void okandKommunGer404() {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.hamta("1480"))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(problem.getDetail()).isEqualTo("Kommun 1480 är inte konfigurerad som producent");
			});
	}
}
