package se.sundsvall.minaarenden.integration.db;

import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.mariadb.MariaDBContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * En omdeploy av en ny version ska aldrig tömma databasen. Testet kör schemat från första versionen (V1, som
 * Gradle-versionen av tjänsten skapade), lägger in data, migrerar till senaste versionen och kontrollerar att datat
 * finns kvar och har fått kommun-id från konfigurationen. Därefter körs migreringen igen, som vid nästa omstart.
 */
class FlywayUppgraderingTest {

	private static final MariaDBContainer MARIADB = new MariaDBContainer("mariadb:11.4");

	private static DriverManagerDataSource dataSource;

	@BeforeAll
	static void startaDatabas() {
		MARIADB.start();
		dataSource = new DriverManagerDataSource(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword());
	}

	@AfterAll
	static void stoppaDatabas() {
		MARIADB.stop();
	}

	@Test
	void dataFranTidigareVersionFinnsKvarEfterUppgradering() {
		flyway("1").migrate();
		final var jdbc = new JdbcTemplate(dataSource);
		jdbc.update("""
			INSERT INTO kundhandelse (kundhandelse_id, producent, kund_identifierare, kund_typ, rubrik, beskrivning, sprak, tidpunkt,
			  kundhandelse_typ, producentarendet_kraver_kundatgard, producentarendet_klart, version, skapad)
			VALUES ('REFKOM-GAMMAL-1', 'Referenskommunen', '199009090000', 'Personnummer', 'Gammal', 'Från V1', 'sv',
			  '2026-01-01 10:00:00', 'REFKOM.TEST.SKAPAD', 0, 0, '6.1', '2026-01-01 10:00:00')""");
		jdbc.update("INSERT INTO kundhandelse_tagg (kundhandelse_ref, tagg) SELECT id, 'gammal' FROM kundhandelse");

		final var resultat = flyway(null).migrate();

		assertThat(resultat.migrationsExecuted).isEqualTo(1);
		assertThat(jdbc.queryForMap("SELECT municipality_id, rubrik, beskrivning, skapad IS NOT NULL AS har_skapad, andrad FROM kundhandelse"))
			.containsEntry("municipality_id", "2262")
			.containsEntry("rubrik", "Gammal")
			.containsEntry("beskrivning", "Från V1")
			.containsEntry("andrad", null);
		assertThat(jdbc.queryForObject("SELECT tagg FROM kundhandelse_tagg", String.class)).isEqualTo("gammal");

		// Ny kommunkolumn saknar standardvärde efter migreringen: nya rader måste ange kommun.
		assertThatException().isThrownBy(() -> jdbc.update("""
			INSERT INTO kundhandelse (kundhandelse_id, producent, rubrik, beskrivning, sprak, tidpunkt, kundhandelse_typ,
			  producentarendet_kraver_kundatgard, producentarendet_klart, version, skapad)
			VALUES ('X', 'p', 'r', 'b', 'sv', NOW(), 'REFKOM.A.B', 0, 0, '6.1', NOW())"""));

		// Nästa omstart: inget att migrera, validering går igenom och datat är orört.
		assertThat(flyway(null).migrate().migrationsExecuted).isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM kundhandelse", Integer.class)).isEqualTo(1);
	}

	private static Flyway flyway(final String target) {
		final var configuration = Flyway.configure()
			.dataSource(dataSource)
			.locations("classpath:db/migration")
			.placeholders(Map.of("default_municipality_id", "2262"))
			.cleanDisabled(true);
		if (target != null) {
			configuration.target(target);
		}
		return configuration.load();
	}
}
