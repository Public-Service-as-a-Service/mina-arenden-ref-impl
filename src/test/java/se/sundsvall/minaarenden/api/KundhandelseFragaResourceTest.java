package se.sundsvall.minaarenden.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.minaarenden.Application;
import se.sundsvall.minaarenden.api.model.Delfraga;
import se.sundsvall.minaarenden.api.model.KundhandelseFragaRequest;
import se.sundsvall.minaarenden.api.model.KundhandelseFragaResponse;
import se.sundsvall.minaarenden.service.FragaService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.minaarenden.service.TestdataFactory.fraga;

@AutoConfigureWebTestClient
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class KundhandelseFragaResourceTest {

	static final String PATH = "/2281/kundhandelseFragaSynkron";
	static final String API_KEY = "junit-fraga-nyckel-0123456789abcdef0123456789";

	@MockitoBean
	private FragaService fragaService;

	@Autowired
	private WebTestClient webTestClient;

	@Captor
	private ArgumentCaptor<KundhandelseFragaRequest> requestCaptor;

	@Test
	void kundhandelseFragaSynkron() {
		final var request = fraga();
		final var svar = KundhandelseFragaResponse.create()
			.withKundhandelser(List.of())
			.withDelfragor(List.of(Delfraga.create().withProducent("Testköpings kommun").withStatus("OK").withHttpCode(200)));
		when(fragaService.besvara(eq("2281"), requestCaptor.capture(), eq("sv"))).thenReturn(svar);

		final var response = webTestClient.post().uri(PATH)
			.contentType(APPLICATION_JSON)
			.header("X-API-Key", API_KEY)
			.header("skv_client_correlation_id", "0002aa29-49f2-4baf-be51-c7c39c9824b4")
			.header("Accept-Language", "sv")
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(KundhandelseFragaResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(svar);
		assertThat(requestCaptor.getValue()).isEqualTo(request);
		verify(fragaService).besvara(eq("2281"), eq(request), eq("sv"));
	}

	@Test
	void correlationIdBehoverInteVaraUuid() {
		when(fragaService.besvara(eq("2281"), requestCaptor.capture(), eq(null))).thenReturn(KundhandelseFragaResponse.create());

		webTestClient.post().uri(PATH)
			.contentType(APPLICATION_JSON)
			.header("X-API-Key", API_KEY)
			.header("skv_client_correlation_id", "anrop-4711")
			.bodyValue(fraga())
			.exchange()
			.expectStatus().isOk();
	}
}
