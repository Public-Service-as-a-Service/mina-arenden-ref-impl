package se.sundsvall.minaarenden.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.minaarenden.Application;
import se.sundsvall.minaarenden.api.model.AntalKundhandelser;
import se.sundsvall.minaarenden.api.model.CachadKundhandelse;
import se.sundsvall.minaarenden.api.model.Kundhandelse;
import se.sundsvall.minaarenden.service.KundhandelseService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.minaarenden.service.TestdataFactory.nyKundhandelse;
import static se.sundsvall.minaarenden.service.TestdataFactory.part;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class KundhandelseResourceTest {

	static final String PATH = "/2281/kundhandelser";
	static final String API_KEY = "junit-admin-nyckel-0123456789abcdef0123456789";

	@MockitoBean
	private KundhandelseService kundhandelseService;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void sparaKundhandelser() {
		final var nya = List.of(nyKundhandelse("TESTKOP-1"), nyKundhandelse("TESTKOP-2"));
		when(kundhandelseService.spara("2281", nya)).thenReturn(2);

		final var svar = webTestClient.post().uri(PATH)
			.contentType(APPLICATION_JSON)
			.header("X-API-Key", API_KEY)
			.bodyValue(nya)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(AntalKundhandelser.class)
			.returnResult()
			.getResponseBody();

		assertThat(svar).isEqualTo(new AntalKundhandelser(2));
		verify(kundhandelseService).spara("2281", nya);
	}

	@Test
	void hamtaKundhandelse() {
		final var cachad = CachadKundhandelse.create().withPart(part()).withKundhandelse(Kundhandelse.create().withKundhandelseId("TESTKOP-1")).withTaggar(List.of("bygglov"));
		when(kundhandelseService.hamta("2281", "TESTKOP-1")).thenReturn(cachad);

		final var svar = webTestClient.get().uri(PATH + "/TESTKOP-1")
			.header("X-API-Key", API_KEY)
			.exchange()
			.expectStatus().isOk()
			.expectBody(CachadKundhandelse.class)
			.returnResult()
			.getResponseBody();

		assertThat(svar).isEqualTo(cachad);
	}

	@Test
	void taBortKundhandelse() {
		webTestClient.delete().uri(PATH + "/TESTKOP-1")
			.header("X-API-Key", API_KEY)
			.exchange()
			.expectStatus().isNoContent();

		verify(kundhandelseService).taBort("2281", "TESTKOP-1");
	}

	@Test
	void antalKundhandelser() {
		when(kundhandelseService.antal("2281")).thenReturn(5L);

		webTestClient.get().uri(PATH)
			.header("X-API-Key", API_KEY)
			.exchange()
			.expectStatus().isOk()
			.expectBody().json("""
				{"antal":5}
				""", true);
	}
}
