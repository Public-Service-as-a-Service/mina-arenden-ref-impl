package se.sundsvall.minaarenden.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import se.sundsvall.dept44.problem.ProblemExceptionHandler;

/**
 * Fel som uppstår redan när anropet matchas mot en controller (fel metod, fel Content-Type), och därför inte når
 * {@link KundhandelseFragaExceptionHandler}. För frågegränssnittet blir svaret {"message": "..."} enligt Skatteverkets
 * API-definition (405, 415); för alla andra sökvägar lämnas felet vidare till dept44:s ProblemExceptionHandler.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class KundhandelseFragaRoutingExceptionHandler {

	private final ProblemExceptionHandler problemExceptionHandler;

	KundhandelseFragaRoutingExceptionHandler(final ProblemExceptionHandler problemExceptionHandler) {
		this.problemExceptionHandler = problemExceptionHandler;
	}

	@ExceptionHandler({
		HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class
	})
	ResponseEntity<?> handleRoutingException(final Exception e, final HttpServletRequest request, final WebRequest webRequest) throws Exception {
		if (ApiPaths.isKundhandelseFraga(request)) {
			return KundhandelseFragaExceptionHandler.svar(((org.springframework.web.ErrorResponse) e).getStatusCode(), null);
		}
		return problemExceptionHandler.handleException(e, webRequest);
	}
}
