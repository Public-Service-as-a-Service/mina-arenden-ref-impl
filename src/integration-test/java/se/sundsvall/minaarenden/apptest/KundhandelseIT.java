package se.sundsvall.minaarenden.apptest;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.minaarenden.Application;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.minaarenden.apptest.Konstanter.ADMIN_NYCKEL;
import static se.sundsvall.minaarenden.apptest.Konstanter.API_KEY_HEADER;
import static se.sundsvall.minaarenden.apptest.Konstanter.FRAGA_NYCKEL;

/**
 * Underhåll av ärendecachen genom hela tjänsten mot MariaDB.
 *
 * @see src/test/resources/db/scripts/testdata-it.sql för testdata.
 */
@WireMockAppTestSuite(files = "classpath:/KundhandelseIT/", classes = Application.class)
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class KundhandelseIT extends AbstractAppTest {

	private static final String PATH = "/2281/kundhandelser";

	@Test
	void test01_sparaKundhandelser() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(API_KEY_HEADER, ADMIN_NYCKEL)
			.withRequest("request.json")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();

		// Den ersatta kundhändelsen har nytt innehåll, den nya finns och tidpunkten lämnas ut med svensk offset.
		setupCall()
			.withServicePath(PATH + "/TESTKOP-BYGG-2026-00123-1")
			.withHttpMethod(GET)
			.withHeader(API_KEY_HEADER, ADMIN_NYCKEL)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("ersatt.json")
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withHeader(API_KEY_HEADER, ADMIN_NYCKEL)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("antal.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_hamtaKundhandelse() {
		setupCall()
			.withServicePath(PATH + "/TESTKOP-BYGG-2026-00123-2")
			.withHttpMethod(GET)
			.withHeader(API_KEY_HEADER, ADMIN_NYCKEL)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_taBortKundhandelse() {
		setupCall()
			.withServicePath(PATH + "/TESTKOP-FSK-2026-00777-1")
			.withHttpMethod(DELETE)
			.withHeader(API_KEY_HEADER, ADMIN_NYCKEL)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(PATH + "/TESTKOP-FSK-2026-00777-1")
			.withHttpMethod(GET)
			.withHeader(API_KEY_HEADER, ADMIN_NYCKEL)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_kundhandelseIAnnanKommunSyns() {
		setupCall()
			.withServicePath("/2262/kundhandelser/TESTKOP-BYGG-2026-00123-1")
			.withHttpMethod(GET)
			.withHeader(API_KEY_HEADER, ADMIN_NYCKEL)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_frageNyckelFarInteLasaIn() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(API_KEY_HEADER, FRAGA_NYCKEL)
			.withRequest("request.json")
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_felPrefix() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(API_KEY_HEADER, ADMIN_NYCKEL)
			.withRequest("request.json")
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}
}
