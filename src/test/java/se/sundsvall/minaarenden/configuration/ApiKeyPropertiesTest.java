package se.sundsvall.minaarenden.configuration;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ApiKeyPropertiesTest {

	private static final String NYCKEL_1 = "a".repeat(ApiKeyProperties.MIN_LANGD);
	private static final String NYCKEL_2 = "b".repeat(64);

	@Test
	void nycklarTrimmasOchDubbletterTasBort() {
		final var properties = new ApiKeyProperties("X-API-Key", List.of(" " + NYCKEL_1 + " ", "", NYCKEL_1), List.of(NYCKEL_2));

		assertThat(properties.fragaKeys()).containsExactly(NYCKEL_1);
		assertThat(properties.adminKeys()).containsExactly(NYCKEL_2);
		assertThat(properties.headerName()).isEqualTo("X-API-Key");
	}

	@Test
	void enSortsNyckelRacker() {
		assertThat(new ApiKeyProperties("X-API-Key", null, List.of(NYCKEL_2)).fragaKeys()).isEmpty();
		assertThat(new ApiKeyProperties("X-API-Key", List.of(NYCKEL_1), null).adminKeys()).isEmpty();
	}

	@Test
	void utanNycklarStartarInteTjansten() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new ApiKeyProperties("X-API-Key", List.of(), List.of(" ")))
			.withMessageContaining("Ingen API-nyckel är konfigurerad");
	}

	@Test
	void forKortNyckelAvvisasUtanAttNyckelnSyns() {
		final var forKort = "hemlig-men-for-kort";

		assertThatIllegalStateException()
			.isThrownBy(() -> new ApiKeyProperties("X-API-Key", List.of(NYCKEL_1), List.of(NYCKEL_2, forKort)))
			.withMessageContaining("Nyckel nummer 2 i API_KEYS_ADMIN")
			.withMessageNotContaining(forKort);
	}

	@Test
	void nyckelMedMellanslagAvvisas() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new ApiKeyProperties("X-API-Key", List.of("a".repeat(20) + " " + "b".repeat(20)), List.of()))
			.withMessageContaining("API_KEYS_FRAGA");
	}

	@Test
	void headerMasteAnges() {
		assertThatIllegalStateException().isThrownBy(() -> new ApiKeyProperties(" ", List.of(NYCKEL_1), List.of()));
		assertThatIllegalStateException().isThrownBy(() -> new ApiKeyProperties(null, List.of(NYCKEL_1), List.of()));
	}
}
