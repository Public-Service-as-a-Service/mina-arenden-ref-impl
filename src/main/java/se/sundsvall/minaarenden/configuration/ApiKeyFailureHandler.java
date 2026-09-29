package se.sundsvall.minaarenden.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ProblemResponse;
import se.sundsvall.minaarenden.api.ApiPaths;
import se.sundsvall.minaarenden.api.model.ErrorResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

/**
 * Svar när API-nyckel saknas eller är ogiltig (401) eller inte ger behörighet till gränssnittet (403).
 * Frågegränssnittet
 * svarar i Skatteverkets format {"message": "..."}, övriga gränssnitt med RFC 9457 som resten av dept44.
 */
@Component
class ApiKeyFailureHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final JsonMapper jsonMapper;
	private final String headerName;

	ApiKeyFailureHandler(final JsonMapper jsonMapper, final ApiKeyProperties properties) {
		this.jsonMapper = jsonMapper;
		this.headerName = properties.headerName();
	}

	@Override
	public void commence(final HttpServletRequest request, final HttpServletResponse response, final AuthenticationException authException) throws IOException {
		response.setHeader("WWW-Authenticate", "ApiKey header=\"" + headerName + "\"");
		skriv(request, response, UNAUTHORIZED, "API-nyckel saknas eller är ogiltig (header " + headerName + ")");
	}

	@Override
	public void handle(final HttpServletRequest request, final HttpServletResponse response, final AccessDeniedException accessDeniedException) throws IOException {
		skriv(request, response, FORBIDDEN, "API-nyckeln ger inte behörighet till den här resursen");
	}

	private void skriv(final HttpServletRequest request, final HttpServletResponse response, final HttpStatus status, final String detalj) throws IOException {
		response.setStatus(status.value());
		if (ApiPaths.isKundhandelseFraga(request)) {
			response.setContentType(APPLICATION_JSON_VALUE);
			jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.create().withMessage(status == UNAUTHORIZED ? "Unauthorized" : "Forbidden"));
		} else {
			response.setContentType(APPLICATION_PROBLEM_JSON_VALUE);
			jsonMapper.writeValue(response.getOutputStream(), new ProblemResponse(Problem.valueOf(status, detalj)));
		}
	}
}
