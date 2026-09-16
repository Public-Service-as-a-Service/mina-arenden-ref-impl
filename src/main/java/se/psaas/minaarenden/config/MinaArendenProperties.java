package se.psaas.minaarenden.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Konfiguration för producenten. Se application.yml och README för miljövariabler.
 */
@ConfigurationProperties(prefix = "minaarenden")
public record MinaArendenProperties(
        String producent,
        String prefix,
        String standardVersion,
        String clientId,
        String clientSecret,
        String adminApiKey,
        boolean seed) {

    public boolean requiresClientCredentials() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    public boolean requiresAdminApiKey() {
        return adminApiKey != null && !adminApiKey.isBlank();
    }
}
