package se.sundsvall.minaarenden.api;

import jakarta.validation.ConstraintViolationException;
import java.time.DateTimeException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.minaarenden.api.model.ErrorResponse;
import se.sundsvall.minaarenden.configuration.CorrelationIdFilter;
import tools.jackson.databind.DatabindException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Felsvar för frågegränssnittet enligt Skatteverkets API-definition: JSON med ett enda fält message (schemat tillåter
 * inga andra fält, därför inte RFC 9457 som i resten av tjänsten). Meddelandena är egna, stabila texter som pekar ut
 * fält; tekniska detaljer ur underliggande undantag stannar i serverloggen.
 *
 * <p>
 * Gäller bara {@link KundhandelseFragaResource} och går före dept44:s ProblemExceptionHandler. Fel som uppstår innan
 * anropet når controllern hanteras av {@link KundhandelseFragaRoutingExceptionHandler} och
 * se.sundsvall.minaarenden.configuration.ApiKeyFailureHandler.
 */
@RestControllerAdvice(assignableTypes = KundhandelseFragaResource.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class KundhandelseFragaExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(KundhandelseFragaExceptionHandler.class);

	/** Parametrar i {@link KundhandelseFragaResource} som motsvarar headrar. */
	private static final Map<String, String> PARAMETER_TILL_FALT = Map.of("correlationId", CorrelationIdFilter.HEADER);

	@ExceptionHandler(ThrowableProblem.class)
	ResponseEntity<ErrorResponse> handleProblem(final ThrowableProblem problem) {
		final var status = Optional.ofNullable(problem.getStatus()).orElse(INTERNAL_SERVER_ERROR);
		if (status.is5xxServerError()) {
			LOG.error("Fel vid besvarande av fråga", problem);
			return svar(status, null);
		}
		return svar(status, problem.getDetail());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> handleNotReadable(final HttpMessageNotReadableException e) {
		final var falt = jsonFalt(e);
		return badRequest(falt == null ? "meddelandet kan inte tolkas som JSON" : "fältet " + falt + " har fel typ eller format", e);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(final MethodArgumentNotValidException e) {
		final var detaljer = e.getBindingResult().getFieldErrors().stream()
			.map(fel -> fel.getField() + ": " + fel.getDefaultMessage())
			.sorted()
			.collect(Collectors.joining("; "));
		return badRequest(detaljer.isEmpty() ? "ogiltigt innehåll" : detaljer, e);
	}

	/**
	 * Validering av metodparametrar (@Validated): municipalityId och headern skv_client_correlation_id. Sökvägen börjar med
	 * metodnamnet, som tas bort; parametrar som motsvarar headrar får headerns namn.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ErrorResponse> handleConstraintViolation(final ConstraintViolationException e) {
		final var detaljer = e.getConstraintViolations().stream()
			.map(v -> faltnamn(v.getPropertyPath().toString()) + ": " + v.getMessage())
			.sorted()
			.collect(Collectors.joining("; "));
		return badRequest(detaljer.isEmpty() ? "ogiltigt innehåll" : detaljer, e);
	}

	static String faltnamn(final String sokvag) {
		final var utanMetod = sokvag.contains(".") ? sokvag.substring(sokvag.indexOf('.') + 1) : sokvag;
		return PARAMETER_TILL_FALT.getOrDefault(utanMetod, utanMetod);
	}

	@ExceptionHandler(DateTimeException.class)
	ResponseEntity<ErrorResponse> handleDateTime(final DateTimeException e) {
		return badRequest("ogiltigt datum eller ogiltig tidpunkt", e);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException e) {
		return badRequest("ogiltigt innehåll", e);
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	ResponseEntity<ErrorResponse> handleMissingHeader(final MissingRequestHeaderException e) {
		return badRequest("headern " + e.getHeaderName() + " saknas", e);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleException(final Exception e) {
		if (e instanceof final org.springframework.web.ErrorResponse errorResponse && !errorResponse.getStatusCode().is5xxServerError()) {
			LOG.debug("Avvisat anrop ({})", errorResponse.getStatusCode().value(), e);
			return svar(errorResponse.getStatusCode(), null);
		}
		LOG.error("Oväntat fel vid besvarande av fråga", e);
		return svar(INTERNAL_SERVER_ERROR, null);
	}

	static ResponseEntity<ErrorResponse> svar(final HttpStatusCode status, final String detaljer) {
		final var rubrik = rubrik(status);
		return ResponseEntity.status(status)
			.contentType(APPLICATION_JSON)
			.body(ErrorResponse.create().withMessage(detaljer == null || detaljer.isBlank() ? rubrik : rubrik + ": " + detaljer));
	}

	/**
	 * Samma texter som i Skatteverkets API-definition.
	 */
	static String rubrik(final HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> "Bad request";
			case 401 -> "Unauthorized";
			case 403 -> "Forbidden";
			case 404 -> "Not found";
			case 405 -> "Method not allowed";
			case 409 -> "Conflict";
			case 415 -> "Unsupported media type";
			case 500 -> "Internal server error";
			default -> Optional.ofNullable(HttpStatus.resolve(status.value())).map(HttpStatus::getReasonPhrase).orElse("Error");
		};
	}

	private static ResponseEntity<ErrorResponse> badRequest(final String detaljer, final Exception e) {
		LOG.debug("Avvisat anrop (400): {}", detaljer, e);
		return svar(BAD_REQUEST, detaljer);
	}

	/**
	 * Fältets sökväg i JSON (t.ex. fraga.parter[0].kund) utan klassnamn, om felet kommer från Jackson.
	 */
	static String jsonFalt(final HttpMessageNotReadableException e) {
		if (!(e.getCause() instanceof final DatabindException databindException) || databindException.getPath().isEmpty()) {
			return null;
		}
		final var sokvag = new StringBuilder();
		for (final var referens : databindException.getPath()) {
			if (referens.getPropertyName() != null) {
				if (!sokvag.isEmpty()) {
					sokvag.append('.');
				}
				sokvag.append(referens.getPropertyName());
			} else if (referens.getIndex() >= 0) {
				sokvag.append('[').append(referens.getIndex()).append(']');
			}
		}
		return sokvag.isEmpty() ? null : sokvag.toString();
	}
}
