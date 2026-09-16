package se.psaas.minaarenden.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Konfiguration för producenten. Se application.yml och README för miljövariabler.
 *
 * <p>Säkerhetskonfigurationen är fail-closed: en halvfärdig konfiguration (bara CLIENT_ID eller bara
 * CLIENT_SECRET) stoppar starten i stället för att tyst stänga av autentiseringen, och med
 * kravAutentisering (profilen production) vägrar applikationen starta utan alla nycklar.
 */
@ConfigurationProperties(prefix = "minaarenden")
public record MinaArendenProperties(
        String producent,
        String prefix,
        String standardVersion,
        String clientId,
        String clientSecret,
        String adminApiKey,
        boolean kravAutentisering,
        boolean seed) {

    public MinaArendenProperties {
        if (harVarde(clientId) != harVarde(clientSecret)) {
            throw new IllegalStateException(
                    "CLIENT_ID och CLIENT_SECRET måste antingen båda sättas eller båda utelämnas; en halv konfiguration skulle lämna /kundhandelseFragaSynkron oskyddad");
        }
        if (kravAutentisering && !(harVarde(clientId) && harVarde(clientSecret) && harVarde(adminApiKey))) {
            throw new IllegalStateException(
                    "MINA_ARENDEN_KRAV_AUTENTISERING=true (profilen production) kräver att CLIENT_ID, CLIENT_SECRET och ADMIN_API_KEY är satta");
        }
        if (!harVarde(prefix) || !prefix.matches("^[A-Z]+$")) {
            throw new IllegalStateException("MINA_ARENDEN_PREFIX ska bestå av versaler A-Z, t.ex. REFKOM");
        }
    }

    public boolean requiresClientCredentials() {
        return harVarde(clientId) && harVarde(clientSecret);
    }

    public boolean requiresAdminApiKey() {
        return harVarde(adminApiKey);
    }

    private static boolean harVarde(String s) {
        return s != null && !s.isBlank();
    }
}
