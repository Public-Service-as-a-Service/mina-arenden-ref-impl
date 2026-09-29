package se.sundsvall.minaarenden.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ApiPathsTest {

	@ParameterizedTest
	@CsvSource(value = {
		"'', /2281/kundhandelseFragaSynkron, true",
		"'', /2281/kundhandelseFragaSynkron/, true",
		"/api, /api/2281/kundhandelseFragaSynkron, true",
		"'', /2281/kundhandelser, false",
		"'', /kundhandelseFragaSynkron, false",
		"'', /2281/kundhandelseFragaSynkron/x, false"
	})
	void isKundhandelseFraga(final String contextPath, final String uri, final boolean forvantat) {
		final var request = new MockHttpServletRequest("POST", uri);
		request.setContextPath(contextPath);

		assertThat(ApiPaths.isKundhandelseFraga(request)).isEqualTo(forvantat);
	}
}
