package se.psaas.minaarenden.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enklast möjliga åtkomstkontroll, avsedd för referens- och testmiljöer.
 *
 * <ul>
 *   <li>/kundhandelseFragaSynkron: kräver headrarna client_id och client_secret om CLIENT_ID och
 *       CLIENT_SECRET är satta (motsvarar headrarna i Skatteverkets API-definition).</li>
 *   <li>/kundhandelser: kräver headern X-Api-Key om ADMIN_API_KEY är satt.</li>
 * </ul>
 *
 * <p>Att nycklarna är satta i par eller inte alls kontrolleras av {@link MinaArendenProperties} vid start.
 * Oskyddade gränssnitt loggas som varning vid start; sätt MINA_ARENDEN_KRAV_AUTENTISERING=true (eller
 * profilen production) för att göra dem obligatoriska. I skarp drift ersätts detta av OAuth 2 enligt
 * Skatteverkets anslutningsvillkor.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final MinaArendenProperties properties;

    public ApiKeyFilter(MinaArendenProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void varnaOmOskyddat() {
        if (!properties.requiresClientCredentials()) {
            log.warn("CLIENT_ID/CLIENT_SECRET är inte satta: /kundhandelseFragaSynkron är öppet utan autentisering");
        }
        if (!properties.requiresAdminApiKey()) {
            log.warn("ADMIN_API_KEY är inte satt: /kundhandelser (inläsning till cachen) är öppet utan autentisering");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/kundhandelseFragaSynkron") && properties.requiresClientCredentials()) {
            boolean ok = equalsConstantTime(properties.clientId(), request.getHeader("client_id"))
                    && equalsConstantTime(properties.clientSecret(), request.getHeader("client_secret"));
            if (!ok) {
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                return;
            }
        }
        if (path.startsWith("/kundhandelser") && properties.requiresAdminApiKey()) {
            if (!equalsConstantTime(properties.adminApiKey(), request.getHeader("X-Api-Key"))) {
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean equalsConstantTime(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
