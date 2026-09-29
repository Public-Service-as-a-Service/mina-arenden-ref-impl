package se.sundsvall.minaarenden.configuration;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

	private final CorrelationIdFilter filter = new CorrelationIdFilter();

	@Test
	void giltigtIdLaggsIMdcUnderAnropet() throws Exception {
		assertThat(mdcUnderAnrop("0002aa29-49f2-4baf-be51-c7c39c9824b4")).isEqualTo("0002aa29-49f2-4baf-be51-c7c39c9824b4");
		assertThat(MDC.get(CorrelationIdFilter.MDC_NYCKEL)).isNull();
	}

	@Test
	void ogiltigtIdLaggsInteIMdc() throws Exception {
		assertThat(mdcUnderAnrop("rad\nbrytning")).isNull();
		assertThat(mdcUnderAnrop("   ")).isNull();
		assertThat(mdcUnderAnrop("x".repeat(201))).isNull();
		assertThat(mdcUnderAnrop(null)).isNull();
	}

	private String mdcUnderAnrop(final String id) throws Exception {
		final var request = new MockHttpServletRequest("POST", "/2281/kundhandelseFragaSynkron");
		if (id != null) {
			request.addHeader(CorrelationIdFilter.HEADER, id);
		}
		final var varde = new AtomicReference<String>();
		filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> varde.set(MDC.get(CorrelationIdFilter.MDC_NYCKEL)));
		return varde.get();
	}
}
