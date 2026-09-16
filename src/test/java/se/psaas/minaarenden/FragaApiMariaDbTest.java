package se.psaas.minaarenden;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Samma API-tester mot riktig MariaDB (samma image som i docker-compose.yml) via Testcontainers.
 * Körs där Docker finns (t.ex. CI) och hoppas annars över, så att H2-testerna fortfarande ger snabb
 * återkoppling lokalt. Fångar skillnader i SQL, kollation, datum och index som H2 inte visar.
 */
@Testcontainers(disabledWithoutDocker = true)
class FragaApiMariaDbTest extends FragaApiTestBase {

    @Container
    @ServiceConnection
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11.4");
}
