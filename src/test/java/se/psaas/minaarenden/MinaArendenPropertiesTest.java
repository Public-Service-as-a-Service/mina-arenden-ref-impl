package se.psaas.minaarenden;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import se.psaas.minaarenden.config.MinaArendenProperties;

/** Säkerhetskonfigurationen ska vara fail-closed: hellre stoppa starten än att tyst lämna gränssnitten öppna. */
class MinaArendenPropertiesTest {

    private static MinaArendenProperties props(String clientId, String clientSecret, String adminKey, boolean krav) {
        return new MinaArendenProperties("Test", "TEST", "6.1", clientId, clientSecret, adminKey, krav, false);
    }

    @Test
    void utanNycklarStartarMenKraverInget() {
        MinaArendenProperties p = props("", "", "", false);
        assertFalse(p.requiresClientCredentials());
        assertFalse(p.requiresAdminApiKey());
    }

    @Test
    void badaNycklarnaAktiverarAutentisering() {
        MinaArendenProperties p = props("id", "hemligt", "nyckel", false);
        assertTrue(p.requiresClientCredentials());
        assertTrue(p.requiresAdminApiKey());
    }

    @Test
    void halvKonfigurationStopparStarten() {
        assertThrows(IllegalStateException.class, () -> props("id", "", "", false));
        assertThrows(IllegalStateException.class, () -> props(null, "hemligt", "", false));
    }

    @Test
    void kravPaAutentiseringStopparStartenUtanNycklar() {
        assertThrows(IllegalStateException.class, () -> props("", "", "", true));
        assertThrows(IllegalStateException.class, () -> props("id", "hemligt", "", true));
        assertTrue(props("id", "hemligt", "nyckel", true).requiresAdminApiKey());
    }

    @Test
    void prefixMasteVaraVersaler() {
        assertThrows(IllegalStateException.class, () -> new MinaArendenProperties("Test", "ref.kom", "6.1", "", "", "", false, false));
    }
}
