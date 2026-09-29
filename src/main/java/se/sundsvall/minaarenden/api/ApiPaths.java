package se.sundsvall.minaarenden.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * Sökvägar till tjänstens gränssnitt. Alla ligger under kommun-id (dept44-konvention), så att en instans kan vara
 * producent för flera kommuner. Vidareförmedlingstjänsten konfigureras med bas-URL:en https://värd/{municipalityId}.
 */
public final class ApiPaths {

	/** Frågegränssnittet enligt Skatteverkets API-definition. */
	public static final String KUNDHANDELSE_FRAGA = "/{municipalityId}/kundhandelseFragaSynkron";

	/** Underhåll av ärendecachen. */
	public static final String KUNDHANDELSER = "/{municipalityId}/kundhandelser";

	private static final Pattern KUNDHANDELSE_FRAGA_MONSTER = Pattern.compile("^/[^/]+/kundhandelseFragaSynkron/?$");

	private ApiPaths() {}

	/**
	 * Frågegränssnittet svarar med felformatet i Skatteverkets API-definition ({"message": "..."}) i stället för RFC 9457,
	 * även för fel som uppstår innan anropet når controllern (401, 403, 405, 415).
	 *
	 * @param  request anropet
	 * @return         true om anropet gäller frågegränssnittet
	 */
	public static boolean isKundhandelseFraga(final HttpServletRequest request) {
		final var uri = request.getRequestURI();
		final var contextPath = request.getContextPath();
		final var path = contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
		return KUNDHANDELSE_FRAGA_MONSTER.matcher(path).matches();
	}
}
