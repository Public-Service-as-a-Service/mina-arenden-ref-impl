package se.psaas.minaarenden.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import se.psaas.minaarenden.api.dto.Granser;

/**
 * Lägger skv_client_correlation_id i MDC (nyckel correlationId) under hela anropet, så att alla loggrader
 * för anropet kan kopplas till anropskedjan hos Skatteverket. Formatet valideras i controllern; här
 * släpps bara skrivbara ASCII-tecken igenom så att loggen inte kan förvanskas av headern.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "skv_client_correlation_id";
    public static final String MDC_NYCKEL = "correlationId";

    /** Skrivbara ASCII-tecken inklusive mellanslag, begränsad längd. */
    public static final Pattern TILLATET_FORMAT = Pattern.compile("^[\\x20-\\x7E]{1," + Granser.MAX_CORRELATION_ID_LANGD + "}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = request.getHeader(HEADER);
        boolean satt = id != null && TILLATET_FORMAT.matcher(id).matches() && !id.isBlank();
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
