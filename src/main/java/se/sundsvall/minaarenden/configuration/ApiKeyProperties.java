package se.sundsvall.minaarenden.configuration;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * API-nycklar. Skyddet är fail-closed: utan minst en giltig nyckel startar inte tjänsten, och det finns inget sätt att
 * stänga av kontrollen. Nycklarna anges som kommaseparerade listor (miljövariablerna API_KEYS_FRAGA och
 * API_KEYS_ADMIN), så att en ny nyckel kan läggas till innan den gamla tas bort vid byte.
 *
 * @param headerName header som nyckeln skickas i
 * @param fragaKeys  nycklar som får anropa /{municipalityId}/kundhandelseFragaSynkron (vidareförmedlingstjänsten)
 * @param adminKeys  nycklar som får anropa allt, även läsa in och ta bort i ärendecachen (verksamhetssystem, drift)
 */
@ConfigurationProperties(prefix = "security.api-key")
public record ApiKeyProperties(
	@DefaultValue(ApiKeyProperties.DEFAULT_HEADER_NAME) String headerName,
	@DefaultValue List<String> fragaKeys,
	@DefaultValue List<String> adminKeys) {

	public static final String DEFAULT_HEADER_NAME = "X-API-Key";

	/** Minsta längd på en nyckel. openssl rand -hex 32 ger 64 tecken. */
	public static final int MIN_LANGD = 32;

	private static final Pattern GILTIG_NYCKEL = Pattern.compile("^[\\x21-\\x7E]{" + MIN_LANGD + ",}$");

	public ApiKeyProperties {
		if (headerName == null || headerName.isBlank()) {
			throw new IllegalStateException("security.api-key.header-name får inte vara tom");
		}
		fragaKeys = normalisera(fragaKeys, "API_KEYS_FRAGA");
		adminKeys = normalisera(adminKeys, "API_KEYS_ADMIN");
		if (fragaKeys.isEmpty() && adminKeys.isEmpty()) {
			throw new IllegalStateException("Ingen API-nyckel är konfigurerad. Sätt API_KEYS_ADMIN och/eller API_KEYS_FRAGA "
				+ "(kommaseparerade, minst " + MIN_LANGD + " tecken per nyckel, t.ex. openssl rand -hex 32)");
		}
	}

	private static List<String> normalisera(final List<String> nycklar, final String miljovariabel) {
		final var resultat = Optional.ofNullable(nycklar).orElse(List.of()).stream()
			.filter(nyckel -> nyckel != null && !nyckel.isBlank())
			.map(String::strip)
			.distinct()
			.toList();
		// Nyckelns värde får aldrig hamna i ett felmeddelande eller en logg, bara dess position.
		for (var i = 0; i < resultat.size(); i++) {
			if (!GILTIG_NYCKEL.matcher(resultat.get(i)).matches()) {
				throw new IllegalStateException("Nyckel nummer " + (i + 1) + " i " + miljovariabel + " är ogiltig: den ska vara minst "
					+ MIN_LANGD + " tecken utan mellanslag och bara innehålla skrivbara ASCII-tecken");
			}
		}
		return resultat;
	}
}
