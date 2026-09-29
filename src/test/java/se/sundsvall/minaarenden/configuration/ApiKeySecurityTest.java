package se.sundsvall.minaarenden.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.minaarenden.Application;
import se.sundsvall.minaarenden.api.model.KundhandelseFragaResponse;
import se.sundsvall.minaarenden.service.FragaService;
import se.sundsvall.minaarenden.service.KundhandelseService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;

/**
 * API-nyckelskyddet genom hela filterkedjan. Nycklarna kommer från profilen junit.
 */
@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ApiKeySecurityTest {

	static final String FRAGA_NYCKEL = "junit-fraga-nyckel-0123456789abcdef0123456789";
	static final String ADMIN_NYCKEL = "junit-admin-nyckel-0123456789abcdef0123456789";

	private static final String HEADER = "X-API-Key";
	private static final String FRAGA_PATH = "/2281/kundhandelseFragaSynkron";
	private static final String KUNDHANDELSER_PATH = "/2281/kundhandelser";
	private static final String FRAGA = """
		{"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}]},"anvandare":"199009090000"}
		""";

	@MockitoBean
	private FragaService fragaService;

	@MockitoBean
	private KundhandelseService kundhandelseService;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void fragaUtanNyckelGer401ISkatteverketsFormat() {
		webTestClient.post().uri(FRAGA_PATH)
			.contentType(APPLICATION_JSON)
			.header("skv_client_correlation_id", "abc")
			.bodyValue(FRAGA)
			.exchange()
			.expectStatus().isUnauthorized()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectHeader().valueEquals("WWW-Authenticate", "ApiKey header=\"X-API-Key\"")
			.expectBody().json("""
				{"message":"Unauthorized"}
				""", true);
	}

	@Test
	void fragaMedFelNyckelGer401() {
		webTestClient.post().uri(FRAGA_PATH)
			.contentType(APPLICATION_JSON)
			.header(HEADER, "fel-nyckel-0123456789abcdef0123456789abcdef")
			.header("skv_client_correlation_id", "abc")
			.bodyValue(FRAGA)
			.exchange()
			.expectStatus().isUnauthorized()
			.expectBody().jsonPath("$.message").isEqualTo("Unauthorized");
	}

	@Test
	void fragaMedFrageOchAdminNyckel() {
		when(fragaService.besvara(eq("2281"), any(), any())).thenReturn(KundhandelseFragaResponse.create());

		for (final var nyckel : new String[] {
			FRAGA_NYCKEL, ADMIN_NYCKEL
		}) {
			webTestClient.post().uri(FRAGA_PATH)
				.contentType(APPLICATION_JSON)
				.header(HEADER, nyckel)
				.header("skv_client_correlation_id", "abc")
				.bodyValue(FRAGA)
				.exchange()
				.expectStatus().isOk();
		}
	}

	@Test
	void arendecachenKraverAdminNyckel() {
		when(kundhandelseService.antal("2281")).thenReturn(3L);

		webTestClient.get().uri(KUNDHANDELSER_PATH)
			.exchange()
			.expectStatus().isUnauthorized()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.status").isEqualTo(401)
			.jsonPath("$.title").isEqualTo("Unauthorized");

		webTestClient.get().uri(KUNDHANDELSER_PATH + "/ID-1")
			.header(HEADER, FRAGA_NYCKEL)
			.exchange()
			.expectStatus().isForbidden()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody().jsonPath("$.status").isEqualTo(403);

		webTestClient.get().uri(KUNDHANDELSER_PATH)
			.header(HEADER, ADMIN_NYCKEL)
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.antal").isEqualTo(3);
	}

	@Test
	void halsaArOppenMenDetaljerKraverAdminNyckel() {
		webTestClient.get().uri("/actuator/health")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.status").isEqualTo("UP")
			.jsonPath("$.components").doesNotExist();

		webTestClient.get().uri("/actuator/health")
			.header(HEADER, ADMIN_NYCKEL)
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.components.db.status").isEqualTo("UP");

		webTestClient.get().uri("/actuator/health/readiness").exchange().expectStatus().isOk();
		webTestClient.get().uri("/actuator/info").exchange().expectStatus().isOk();
	}

	@Test
	void ovrigaActuatorEndpointsArStangdaEllerKraverAdminNyckel() {
		webTestClient.get().uri("/actuator/flyway").exchange().expectStatus().isUnauthorized();
		webTestClient.get().uri("/actuator/flyway").header(HEADER, FRAGA_NYCKEL).exchange().expectStatus().isForbidden();
		webTestClient.get().uri("/actuator/flyway").header(HEADER, ADMIN_NYCKEL).exchange().expectStatus().isOk();

		for (final var endpoint : new String[] {
			"env", "beans", "configprops", "loggers", "heapdump", "threaddump", "mappings"
		}) {
			webTestClient.get().uri("/actuator/" + endpoint).header(HEADER, ADMIN_NYCKEL).exchange().expectStatus().isNotFound();
		}
	}

	@Test
	void dokumentationenArOppen() {
		webTestClient.get().uri("/api-docs").exchange().expectStatus().isOk();
		webTestClient.get().uri("/").exchange().expectStatus().isOk();
	}

	@Test
	void okandSokvagKraverNyckel() {
		webTestClient.get().uri("/finns-inte").exchange().expectStatus().isUnauthorized();
		webTestClient.get().uri("/finns-inte").header(HEADER, ADMIN_NYCKEL).exchange().expectStatus().isNotFound();
	}

	@Test
	void felMetodOchFelInnehallstypPaFragan() {
		webTestClient.get().uri(FRAGA_PATH)
			.header(HEADER, FRAGA_NYCKEL)
			.exchange()
			.expectStatus().isEqualTo(405)
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody().json("""
				{"message":"Method not allowed"}
				""", true);

		webTestClient.post().uri(FRAGA_PATH)
			.header(HEADER, FRAGA_NYCKEL)
			.header("skv_client_correlation_id", "abc")
			.contentType(TEXT_PLAIN)
			.bodyValue("text")
			.exchange()
			.expectStatus().isEqualTo(415)
			.expectBody().json("""
				{"message":"Unsupported media type"}
				""", true);
	}

	@Test
	void felMetodPaArendecachenGerProblem() {
		webTestClient.put().uri(KUNDHANDELSER_PATH)
			.header(HEADER, ADMIN_NYCKEL)
			.contentType(APPLICATION_JSON)
			.bodyValue("[]")
			.exchange()
			.expectStatus().isEqualTo(405)
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody().jsonPath("$.status").isEqualTo(405);
	}
}
