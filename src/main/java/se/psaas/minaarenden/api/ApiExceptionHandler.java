package se.psaas.minaarenden.api;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import se.psaas.minaarenden.api.dto.ErrorResponse;
import se.psaas.minaarenden.service.OgiltigFragaException;
import se.psaas.minaarenden.service.SaknasException;

/** Felsvar enligt API-specifikationen: JSON med ett fält message. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({OgiltigFragaException.class, HttpMessageNotReadableException.class, ConstraintViolationException.class,
            IllegalArgumentException.class, java.time.DateTimeException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(Exception e) {
        return new ErrorResponse("Bad request: " + rootMessage(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse validation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted()
                .reduce((a, b) -> a + "; " + b)
                .orElse("ogiltigt innehåll");
        return new ErrorResponse("Bad request: " + details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse methodValidation(HandlerMethodValidationException e) {
        return new ErrorResponse("Bad request: " + e.getAllErrors().stream()
                .map(err -> err.getDefaultMessage())
                .sorted()
                .reduce((a, b) -> a + "; " + b)
                .orElse("ogiltigt innehåll"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse missingHeader(MissingRequestHeaderException e) {
        return new ErrorResponse("Bad request: headern " + e.getHeaderName() + " saknas");
    }

    @ExceptionHandler({SaknasException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse notFound(Exception e) {
        return new ErrorResponse("Not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse methodNotAllowed(Exception e) {
        return new ErrorResponse("Method not allowed");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ErrorResponse unsupportedMediaType(Exception e) {
        return new ErrorResponse("Unsupported media type");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse internal(Exception e) {
        log.error("Oväntat fel", e);
        return new ErrorResponse("Internal server error");
    }

    private static String rootMessage(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) {
            r = r.getCause();
        }
        String m = r.getMessage();
        return m == null ? "ogiltigt innehåll" : m.lines().findFirst().orElse(m);
    }
}
