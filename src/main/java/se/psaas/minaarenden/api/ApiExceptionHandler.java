package se.psaas.minaarenden.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolationException;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import se.psaas.minaarenden.api.dto.ErrorResponse;
import se.psaas.minaarenden.service.OgiltigFragaException;
import se.psaas.minaarenden.service.SaknasException;

/**
 * Felsvar enligt API-specifikationen: JSON med ett enda fält message (schemat tillåter inga andra
 * fält, därför inte RFC 9457 ProblemDetail). Meddelandena till klienten är egna, stabila texter som
 * pekar ut fält; tekniska detaljer ur underliggande undantag stannar i serverloggen.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(OgiltigFragaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse ogiltigFraga(OgiltigFragaException e) {
        return badRequest(e.getMessage(), e);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse notReadable(HttpMessageNotReadableException e) {
        String falt = jsonFalt(e);
        return badRequest(falt == null ? "meddelandet kan inte tolkas som JSON" : "fältet " + falt + " har fel typ eller format", e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse validation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return badRequest(details.isEmpty() ? "ogiltigt innehåll" : details, e);
    }

    /** Validering av metodparametrar, t.ex. listan i POST /kundhandelser: fältväg med listindex, [2].rubrik. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse methodValidation(HandlerMethodValidationException e) {
        List<String> details = new ArrayList<>();
        for (ParameterValidationResult r : e.getParameterValidationResults()) {
            String index = r.getContainerIndex() == null ? "" : "[" + r.getContainerIndex() + "]";
            if (r instanceof ParameterErrors pe) {
                pe.getFieldErrors().forEach(f -> details.add(index + "." + f.getField() + ": " + f.getDefaultMessage()));
                pe.getGlobalErrors().forEach(g -> details.add(index + ": " + g.getDefaultMessage()));
            } else {
                r.getResolvableErrors().forEach(m -> details.add(index.isEmpty() ? m.getDefaultMessage() : index + ": " + m.getDefaultMessage()));
            }
        }
        String text = details.stream().sorted().collect(Collectors.joining("; "));
        return badRequest(text.isEmpty() ? "ogiltigt innehåll" : text, e);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse constraintViolation(ConstraintViolationException e) {
        String details = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return badRequest(details.isEmpty() ? "ogiltigt innehåll" : details, e);
    }

    @ExceptionHandler(DateTimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse dateTime(DateTimeException e) {
        return badRequest("ogiltigt datum eller ogiltig tidpunkt", e);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse illegalArgument(IllegalArgumentException e) {
        return badRequest("ogiltigt innehåll", e);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse missingHeader(MissingRequestHeaderException e) {
        return badRequest("headern " + e.getHeaderName() + " saknas", e);
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

    /** Samtidig inläsning av samma nya kundhandelseId: det unika villkoret i databasen vinner, anropet kan göras om. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse conflict(DataIntegrityViolationException e) {
        log.warn("Databasvillkor bröts, troligen samtidig inläsning av samma kundhandelseId: {}", e.getMostSpecificCause().getMessage());
        return new ErrorResponse("Conflict: samtidig ändring av samma kundhändelse, skicka anropet igen");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse internal(Exception e) {
        log.error("Oväntat fel", e);
        return new ErrorResponse("Internal server error");
    }

    private static ErrorResponse badRequest(String detaljer, Exception e) {
        log.debug("Avvisat anrop (400): {}", detaljer, e);
        return new ErrorResponse("Bad request: " + detaljer);
    }

    /** Fältets sökväg i JSON (t.ex. fraga.parter[0].kund) utan klassnamn, om felet kommer från Jackson. */
    private static String jsonFalt(HttpMessageNotReadableException e) {
        Throwable orsak = e.getCause();
        if (!(orsak instanceof JsonMappingException jme) || jme.getPath().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference ref : jme.getPath()) {
            if (ref.getFieldName() != null) {
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
