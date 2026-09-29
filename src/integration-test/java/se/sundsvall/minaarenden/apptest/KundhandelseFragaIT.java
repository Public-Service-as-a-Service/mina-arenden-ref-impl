package se.sundsvall.minaarenden.apptest;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.minaarenden.Application;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.minaarenden.apptest.Konstanter.ADMIN_NYCKEL;
import static se.sundsvall.minaarenden.apptest.Konstanter.API_KEY_HEADER;
import static se.sundsvall.minaarenden.apptest.Konstanter.CORRELATION_ID;
import static se.sundsvall.minaarenden.apptest.Konstanter.CORRELATION_ID_HEADER;
import static se.sundsvall.minaarenden.apptest.Konstanter.FRAGA_NYCKEL;

/**
 * Frågegränssnittet genom hela tjänsten mot MariaDB.
 *
 * @see src/test/resources/db/scripts/testdata-it.sql för testdata.
 */
@WireMockAppTestSuite(files = "classpath:/KundhandelseFragaIT/", classes = Application.class)
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class KundhandelseFragaIT extends AbstractAppTest {

	private static final String PATH = "/2281/kundhandelseFragaSynkron";

	@Test
	void test01_fragaPaKund() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(API_KEY_HEADER, FRAGA_NYCKEL)
			.withHeader(CORRELATION_ID_HEADER, CORRELATION_ID)
			.withHeader("Accept-Language", "sv")
			.withRequest("request.json")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_fragaMedFilterSorteringOchPaginering() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(API_KEY_HEADER, ADMIN_NYCKEL)
			.withHeader(CORRELATION_ID_HEADER, CORRELATION_ID)
			.withRequest("request.json")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_fragaUtanApiNyckel() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(CORRELATION_ID_HEADER, CORRELATION_ID)
			.withRequest("request.json")
			.withExpectedResponseStatus(UNAUTHORIZED)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_ogiltigFraga() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(API_KEY_HEADER, FRAGA_NYCKEL)
			.withHeader(CORRELATION_ID_HEADER, CORRELATION_ID)
			.withRequest("request.json")
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_kommunSomInteArKonfigurerad() {
		setupCall()
			.withServicePath("/1480/kundhandelseFragaSynkron")
			.withHttpMethod(POST)
			.withHeader(API_KEY_HEADER, FRAGA_NYCKEL)
			.withHeader(CORRELATION_ID_HEADER, CORRELATION_ID)
			.withRequest("request.json")
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}
}
