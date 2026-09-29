package se.sundsvall.minaarenden.apptest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.minaarenden.Application;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.minaarenden.apptest.Konstanter.ADMIN_NYCKEL;
import static se.sundsvall.minaarenden.apptest.Konstanter.API_KEY_HEADER;
import static se.sundsvall.minaarenden.apptest.Konstanter.CORRELATION_ID;
import static se.sundsvall.minaarenden.apptest.Konstanter.CORRELATION_ID_HEADER;
import static se.sundsvall.minaarenden.apptest.Konstanter.FRAGA_NYCKEL;

/**
 * Frågans regler (standarden och Skatteverkets API-definition) mot MariaDB, inklusive ordning i svaren.
 *
 * @see src/test/resources/db/scripts/testdata-it.sql för testdata.
 */
@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("it")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class KundhandelseFragaBeteendeIT {

	private static final String KUND = """
		{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}""";

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void kundhandelseTypMatcharPrefixOchExaktTyp() {
		fraga("\"kundhandelseTyper\":[\"TESTKOP.BYGGLOV\"]").jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(2);
		fraga("\"kundhandelseTyper\":[\"TESTKOP.BYGGLOV.ANSOKAN_MOTTAGEN\"]").jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(1);
		fraga("\"kundhandelseTyper\":[\"TESTKOP.BYGG\"]").jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(0);
		fraga("\"kundhandelseTyper\":[\"TESTKOP\"]").jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(3);
	}

	@Test
	void taggarOchDatum() {
		fraga("\"taggar\":[\"kundatgard\"]").jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(2);
		fraga("\"startDatum\":\"2026-09-01\",\"slutDatum\":\"2026-09-02\"")
			.jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(1)
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[0].kundhandelseId").isEqualTo("TESTKOP-BYGG-2026-00123-2");
		// Datumgränserna är inklusive och i svensk tid: 2026-09-10 07:30 svensk tid är 05:30 UTC.
		fraga("\"startDatum\":\"2026-09-10\",\"slutDatum\":\"2026-09-10\"").jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(1);
	}

	@Test
	void sorteringOchPaginering() {
		fraga("\"behandling\":{\"sortering\":[{\"attribut\":\"TIDPUNKT\",\"stigande\":true}],\"paginering\":{\"offset\":1,\"limit\":1}}")
			.jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(3)
			.jsonPath("$.kundhandelser[0].kundhandelserForPart.length()").isEqualTo(1)
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[0].kundhandelseId").isEqualTo("TESTKOP-BYGG-2026-00123-2")
			.jsonPath("$.metadata.behandling.paginering.limit").isEqualTo(1);

		// Utan limit lämnas resten av listan; rubrik sorteras skiftlägesokänsligt.
		fraga("\"behandling\":{\"sortering\":[{\"attribut\":\"RUBRIK\",\"stigande\":true}],\"paginering\":{\"offset\":1}}")
			.jsonPath("$.kundhandelser[0].kundhandelserForPart.length()").isEqualTo(2)
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[0].rubrik").isEqualTo("Ansökan om bygglov mottagen")
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[1].rubrik").isEqualTo("Plats i förskola erbjuden");

		fraga("\"behandling\":{\"sortering\":[{\"attribut\":\"TIDPUNKT\",\"stigande\":false}],\"paginering\":{\"offset\":10,\"limit\":5}}")
			.jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(3)
			.jsonPath("$.kundhandelser[0].kundhandelserForPart.length()").isEqualTo(0);
	}

	@Test
	void fleraParterBesvarasVarForSigOchBaraForKommunen() {
		webTestClient.post().uri("/2281/kundhandelseFragaSynkron")
			.contentType(APPLICATION_JSON)
			.header(API_KEY_HEADER, FRAGA_NYCKEL)
			.header(CORRELATION_ID_HEADER, CORRELATION_ID)
			.bodyValue("""
				{"fraga":{"parter":[%s,{"kund":{"identifierare":"165560001234","typ":"Organisationsnummer"}},{"arende":{"identifierare":"FINNS-INTE","typ":"Diarienummer"}}]},"anvandare":"199009090000"}
				""".formatted(KUND))
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.kundhandelser.length()").isEqualTo(3)
			.jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(3)
			.jsonPath("$.kundhandelser[1].totaltAntalKundhandelser").isEqualTo(2)
			.jsonPath("$.kundhandelser[2].totaltAntalKundhandelser").isEqualTo(0)
			.jsonPath("$.delfragor.length()").isEqualTo(3);

		// Samma kund i kommun 2262 ger bara Timrås kundhändelse.
		webTestClient.post().uri("/2262/kundhandelseFragaSynkron")
			.contentType(APPLICATION_JSON)
			.header(API_KEY_HEADER, FRAGA_NYCKEL)
			.header(CORRELATION_ID_HEADER, CORRELATION_ID)
			.bodyValue("{\"fraga\":{\"parter\":[" + KUND + "]},\"anvandare\":\"199009090000\"}")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.kundhandelser[0].totaltAntalKundhandelser").isEqualTo(1)
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[0].producent").isEqualTo("Timrå kommun")
			.jsonPath("$.delfragor[0].producent").isEqualTo("Timrå kommun");
	}

	@Test
	void tidpunktSorterasKronologisktAvenNarOffsetSkiljer() {
		// Timmen 02:00-03:00 den 25 oktober 2026 finns två gånger i svensk tid. Som text sorterar "+01:00" före "+02:00",
		// men kronologiskt kommer +02:00 (sommartid) först. Sorteringen ska gå på tidpunkten, inte texten.
		webTestClient.post().uri("/2281/kundhandelser")
			.contentType(APPLICATION_JSON)
			.header(API_KEY_HEADER, ADMIN_NYCKEL)
			.bodyValue("""
				[{"kundhandelseId":"DST-VINTER","part":{"kund":{"identifierare":"197001010001","typ":"Personnummer"}},
				  "rubrik":"Efter omställning","beskrivning":"b","tidpunkt":"2026-10-25T02:30:00+01:00",
				  "kundhandelseTyp":"TESTKOP.TEST.SKAPAD","producentarendetKraverKundatgard":false,"producentarendetKlart":false},
				 {"kundhandelseId":"DST-SOMMAR","part":{"kund":{"identifierare":"197001010001","typ":"Personnummer"}},
				  "rubrik":"Före omställning","beskrivning":"b","tidpunkt":"2026-10-25T02:30:00+02:00",
				  "kundhandelseTyp":"TESTKOP.TEST.SKAPAD","producentarendetKraverKundatgard":false,"producentarendetKlart":false}]
				""")
			.exchange()
			.expectStatus().isCreated();

		webTestClient.post().uri("/2281/kundhandelseFragaSynkron")
			.contentType(APPLICATION_JSON)
			.header(API_KEY_HEADER, FRAGA_NYCKEL)
			.header(CORRELATION_ID_HEADER, CORRELATION_ID)
			.bodyValue("""
				{"fraga":{"parter":[{"kund":{"identifierare":"197001010001","typ":"Personnummer"}}],
				 "behandling":{"sortering":[{"attribut":"TIDPUNKT","stigande":true}]}},"anvandare":"197001010001"}
				""")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[0].kundhandelseId").isEqualTo("DST-SOMMAR")
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[0].tidpunkt").isEqualTo("2026-10-25T02:30:00+02:00")
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[1].kundhandelseId").isEqualTo("DST-VINTER")
			.jsonPath("$.kundhandelser[0].kundhandelserForPart[1].tidpunkt").isEqualTo("2026-10-25T02:30:00+01:00");
	}

	@Test
	void sammaKundhandelseIdErsatterIStalletForAttDubblera() {
		final var ny = """
			[{"kundhandelseId":"TEST-1","part":{"kund":{"identifierare":"198001010101","typ":"Personnummer"}},
			  "rubrik":"%s","beskrivning":"Beskrivning","tidpunkt":"2026-09-15 12:00:00",
			  "kundhandelseTyp":"TESTKOP.TEST.SKAPAD","producentarendetKraverKundatgard":false,"producentarendetKlart":false}]
			""";
		for (final var rubrik : new String[] {
			"Testhändelse", "Uppdaterad"
		}) {
			webTestClient.post().uri("/2281/kundhandelser")
				.contentType(APPLICATION_JSON)
				.header(API_KEY_HEADER, ADMIN_NYCKEL)
				.bodyValue(ny.formatted(rubrik))
				.exchange()
				.expectStatus().isCreated();
		}

		webTestClient.get().uri("/2281/kundhandelser/TEST-1")
			.header(API_KEY_HEADER, ADMIN_NYCKEL)
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.kundhandelse.rubrik").isEqualTo("Uppdaterad")
			.jsonPath("$.kundhandelse.tidpunkt").isEqualTo("2026-09-15T12:00:00+02:00");

		webTestClient.get().uri("/2281/kundhandelser")
			.header(API_KEY_HEADER, ADMIN_NYCKEL)
			.exchange()
			.expectBody().jsonPath("$.antal").isEqualTo(6);
	}

	private WebTestClient.BodyContentSpec fraga(final String filter) {
		return webTestClient.post().uri("/2281/kundhandelseFragaSynkron")
			.contentType(APPLICATION_JSON)
			.header(API_KEY_HEADER, FRAGA_NYCKEL)
			.header(CORRELATION_ID_HEADER, CORRELATION_ID)
			.bodyValue("{\"fraga\":{\"parter\":[" + KUND + "]," + filter + "},\"anvandare\":\"199009090000\"}")
			.exchange()
			.expectStatus().isOk()
			.expectBody();
	}
}
