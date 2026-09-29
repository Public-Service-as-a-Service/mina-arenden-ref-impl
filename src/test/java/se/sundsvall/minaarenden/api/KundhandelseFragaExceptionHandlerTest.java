package se.sundsvall.minaarenden.api;

import java.time.DateTimeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.minaarenden.api.model.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;

class KundhandelseFragaExceptionHandlerTest {

	private final KundhandelseFragaExceptionHandler handler = new KundhandelseFragaExceptionHandler();

	@Test
	void problemMedServerfelVisarInteDetaljer() {
		final var svar = handler.handleProblem(Problem.valueOf(BAD_GATEWAY, "intern detalj"));

		assertThat(svar.getStatusCode()).isEqualTo(BAD_GATEWAY);
		assertThat(svar.getBody()).isEqualTo(ErrorResponse.create().withMessage("Bad Gateway"));
	}

	@Test
	void problemUtanStatusBlir500() {
		final var svar = handler.handleProblem(new ThrowableProblem(null, "titel", (HttpStatus) null, "detalj", null));

		assertThat(svar.getStatusCode().value()).isEqualTo(500);
		assertThat(svar.getBody().getMessage()).isEqualTo("Internal server error");
	}

	@Test
	void problemMedKlientfel() {
		assertThat(handler.handleProblem(Problem.valueOf(CONFLICT, "samtidig ändring")).getBody().getMessage()).isEqualTo("Conflict: samtidig ändring");
	}

	@Test
	void ovrigaFel() {
		assertThat(handler.handleDateTime(new DateTimeException("x")).getBody().getMessage()).isEqualTo("Bad request: ogiltigt datum eller ogiltig tidpunkt");
		assertThat(handler.handleIllegalArgument(new IllegalArgumentException("x")).getBody().getMessage()).isEqualTo("Bad request: ogiltigt innehåll");
		assertThat(handler.handleException(new RuntimeException("x")).getStatusCode().value()).isEqualTo(500);
		assertThat(handler.handleException(new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE)).getBody().getMessage()).isEqualTo("Not Acceptable");
		assertThat(handler.handleException(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).getBody().getMessage()).isEqualTo("Internal server error");
	}

	@Test
	void faltnamn() {
		assertThat(KundhandelseFragaExceptionHandler.faltnamn("kundhandelseFragaSynkron.correlationId")).isEqualTo("skv_client_correlation_id");
		assertThat(KundhandelseFragaExceptionHandler.faltnamn("kundhandelseFragaSynkron.municipalityId")).isEqualTo("municipalityId");
		assertThat(KundhandelseFragaExceptionHandler.faltnamn("falt")).isEqualTo("falt");
	}

	@Test
	void rubriker() {
		assertThat(KundhandelseFragaExceptionHandler.rubrik(HttpStatus.UNAUTHORIZED)).isEqualTo("Unauthorized");
		assertThat(KundhandelseFragaExceptionHandler.rubrik(HttpStatus.FORBIDDEN)).isEqualTo("Forbidden");
		assertThat(KundhandelseFragaExceptionHandler.rubrik(HttpStatus.I_AM_A_TEAPOT)).isEqualTo("I'm a teapot");
		assertThat(KundhandelseFragaExceptionHandler.rubrik(HttpStatusCode.valueOf(599))).isEqualTo("Error");
	}
}
