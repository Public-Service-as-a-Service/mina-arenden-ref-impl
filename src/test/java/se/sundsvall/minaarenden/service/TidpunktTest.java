package se.sundsvall.minaarenden.service;

import java.time.DateTimeException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TidpunktTest {

	@ParameterizedTest
	@CsvSource({
		"2026-09-15T12:00:00+02:00, 2026-09-15T10:00:00Z",
		"2026-09-15T10:00:00Z, 2026-09-15T10:00:00Z",
		"2026-09-15T12:00:00, 2026-09-15T10:00:00Z",
		"2026-09-15 12:00:00, 2026-09-15T10:00:00Z",
		"2026-12-15 12:00:00, 2026-12-15T11:00:00Z"
	})
	void tolka(final String varde, final String forvantat) {
		assertThat(Tidpunkt.tolka(varde)).isEqualTo(Instant.parse(forvantat));
	}

	@Test
	void tolkaOgiltig() {
		assertThatExceptionOfType(DateTimeException.class).isThrownBy(() -> Tidpunkt.tolka("igår"));
	}

	@Test
	void formateraMedSvenskOffset() {
		assertThat(Tidpunkt.formatera(Instant.parse("2026-09-15T10:00:00Z"))).isEqualTo("2026-09-15T12:00:00+02:00");
		assertThat(Tidpunkt.formatera(Instant.parse("2026-12-15T11:00:00Z"))).isEqualTo("2026-12-15T12:00:00+01:00");
	}

	@Test
	void dagensStartOchSlutISvenskTid() {
		assertThat(Tidpunkt.dagensStart("2026-09-01")).isEqualTo(Instant.parse("2026-08-31T22:00:00Z"));
		assertThat(Tidpunkt.dagensSlut("2026-09-01")).isEqualTo(Instant.parse("2026-09-01T21:59:59.999Z"));
		assertThat(Tidpunkt.dagensStart(null)).isNull();
		assertThat(Tidpunkt.dagensSlut(null)).isNull();
	}
}
