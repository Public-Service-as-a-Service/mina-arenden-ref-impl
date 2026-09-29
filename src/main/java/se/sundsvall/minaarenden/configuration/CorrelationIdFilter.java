package se.sundsvall.minaarenden.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import se.sundsvall.minaarenden.api.validation.Granser;

/**
 * Lägger skv_client_correlation_id i MDC (nyckel skvClientCorrelationId) under hela anropet, så att loggraderna kan
 * kopplas till anropskedjan hos Skatteverket. dept44:s x-request-id finns kvar i MDC bredvid. Formatet valideras i
 * controllern; här släpps bara giltiga värden igenom så att loggen inte kan förvanskas via headern.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "skv_client_correlation_id";
	public static final String MDC_NYCKEL = "skvClientCorrelationId";

	private static final Pattern TILLATET_FORMAT = Pattern.compile(Granser.CORRELATION_ID_MONSTER);

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException {
		final var id = request.getHeader(HEADER);
		final var satt = id != null && TILLATET_FORMAT.matcher(id).matches();
		if (satt) {
			MDC.put(MDC_NYCKEL, id);
		}
		try {
			chain.doFilter(request, response);
		} finally {
			if (satt) {
				MDC.remove(MDC_NYCKEL);
			}
		}
	}
}
