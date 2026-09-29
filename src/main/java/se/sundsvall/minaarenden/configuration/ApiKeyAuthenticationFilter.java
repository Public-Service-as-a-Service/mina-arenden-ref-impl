package se.sundsvall.minaarenden.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import se.sundsvall.dept44.util.LogUtils;

/**
 * Läser API-nyckeln ur headern och sätter säkerhetskontexten om nyckeln är giltig. Beslutet om anropet får passera
 * fattas av reglerna i {@link ApiKeySecurityConfiguration}; saknad eller ogiltig nyckel ger där 401.
 *
 * <p>
 * Nyckelns id (t.ex. admin-1, aldrig nyckeln) läggs i MDC som apiKeyId under anropet, så att loggrader kan kopplas till
 * den som anropade.
 */
class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

	static final String MDC_API_KEY_ID = "apiKeyId";

	private static final Logger LOG = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

	private final ApiKeyAuthenticator authenticator;
	private final String headerName;

	ApiKeyAuthenticationFilter(final ApiKeyAuthenticator authenticator, final String headerName) {
		this.authenticator = authenticator;
		this.headerName = headerName;
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException {
		final var nyckel = request.getHeader(headerName);
		var satt = false;
		if (nyckel != null) {
			final var autentisering = authenticator.autentisera(nyckel);
			if (autentisering.isPresent()) {
				final var context = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
				context.setAuthentication(autentisering.get());
				SecurityContextHolder.getContextHolderStrategy().setContext(context);
				MDC.put(MDC_API_KEY_ID, autentisering.get().getName());
				satt = true;
			} else {
				LOG.warn("Ogiltig API-nyckel i anrop {} {}", request.getMethod(), LogUtils.sanitizeForLogging(request.getRequestURI()));
			}
		}
		try {
			chain.doFilter(request, response);
		} finally {
			if (satt) {
				MDC.remove(MDC_API_KEY_ID);
				SecurityContextHolder.getContextHolderStrategy().clearContext();
			}
		}
	}
}
