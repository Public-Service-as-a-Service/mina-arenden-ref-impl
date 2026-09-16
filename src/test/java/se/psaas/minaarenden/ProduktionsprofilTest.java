package se.psaas.minaarenden;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import se.psaas.minaarenden.config.MinaArendenProperties;

/**
 * Med profilen production ska applikationen vägra starta utan fullständig autentiseringskonfiguration
 * och aldrig läsa in exempeldata. Körs mot huvudkonfigurationen (application.yml) utan testprofilen,
 * med H2 som databas via kommandoradsargument.
 */
class ProduktionsprofilTest {

    private static final String[] H2 = {
            "--spring.datasource.url=jdbc:h2:mem:produktionsprofil;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
            "--spring.datasource.username=sa", "--spring.datasource.password=", "--spring.datasource.driver-class-name=org.h2.Driver"};

    private static SpringApplicationBuilder produktion() {
        return new SpringApplicationBuilder(MinaArendenApplication.class).web(WebApplicationType.NONE).profiles("production");
    }

    private static String[] args(String... extra) {
        String[] alla = new String[H2.length + extra.length];
        System.arraycopy(H2, 0, alla, 0, H2.length);
        System.arraycopy(extra, 0, alla, H2.length, extra.length);
        return alla;
    }

    @Test
    void produktionUtanNycklarStartarInte() {
        Exception e = assertThrows(Exception.class, () -> produktion().run(args()));
        String orsak = rotorsak(e).getMessage();
        assertTrue(orsak.contains("MINA_ARENDEN_KRAV_AUTENTISERING"), orsak);
    }

    @Test
    void produktionMedNycklarStartarOchLaserInteInExempeldata() {
        // Motsvarar miljövariabeln MINA_ARENDEN_SEED=true, som platshållaren i application.yml läser.
        System.setProperty("MINA_ARENDEN_SEED", "true");
        try (ConfigurableApplicationContext ctx = produktion().run(args(
                "--minaarenden.client-id=id", "--minaarenden.client-secret=hemligt", "--minaarenden.admin-api-key=nyckel"))) {
            MinaArendenProperties props = ctx.getBean(MinaArendenProperties.class);
            assertTrue(props.kravAutentisering());
            assertFalse(props.seed(), "profilen production ska stänga av exempeldata även om MINA_ARENDEN_SEED=true");
        } finally {
            System.clearProperty("MINA_ARENDEN_SEED");
        }
    }

    private static Throwable rotorsak(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) {
            r = r.getCause();
        }
        return r;
    }
}
