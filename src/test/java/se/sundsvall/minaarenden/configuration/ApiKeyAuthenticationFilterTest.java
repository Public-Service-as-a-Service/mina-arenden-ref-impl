package se.sundsvall.minaarenden.configuration;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthenticationFilterTest {

	private static final String NYCKEL = "n".repeat(40);

	private final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
		new ApiKeyAuthenticator(new ApiKeyProperties("X-API-Key", List.of(NYCKEL), List.of())), "X-API-Key");

	@AfterEach
	void rensa() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void giltigNyckelSatterKontextOchMdcUnderAnropet() throws Exception {
		final var autentisering = new AtomicReference<Authentication>();
		final var mdc = new AtomicReference<String>();

		filter.doFilter(request(NYCKEL), new MockHttpServletResponse(), (req, res) -> {
			autentisering.set(SecurityContextHolder.getContext().getAuthentication());
			mdc.set(MDC.get(ApiKeyAuthenticationFilter.MDC_API_KEY_ID));
		});

		assertThat(autentisering.get().getName()).isEqualTo("fraga-1");
		assertThat(mdc.get()).isEqualTo("fraga-1");
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		assertThat(MDC.get(ApiKeyAuthenticationFilter.MDC_API_KEY_ID)).isNull();
	}

	@Test
	void ogiltigEllerSaknadNyckelLamnarKontextenTom() throws Exception {
		for (final var nyckel : new String[] {
			"fel", null
		}) {
			final var autentisering = new AtomicReference<Authentication>();
			filter.doFilter(request(nyckel), new MockHttpServletResponse(), (req, res) -> autentisering.set(SecurityContextHolder.getContext().getAuthentication()));
			assertThat(autentisering.get()).isNull();
		}
	}

	private static MockHttpServletRequest request(final String nyckel) {
		final var request = new MockHttpServletRequest("GET", "/2281/kundhandelser");
		if (nyckel != null) {
			request.addHeader("X-API-Key", nyckel);
		}
		return request;
	}
}
