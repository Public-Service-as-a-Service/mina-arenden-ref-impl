package se.sundsvall.minaarenden.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Tidpunkter enligt RFC 3339 i svar; toleranta vid inläsning. Datumgränser tolkas i svensk tid.
 */
public final class Tidpunkt {

	public static final ZoneId ZON = ZoneId.of("Europe/Stockholm");

	private static final DateTimeFormatter LOKAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private Tidpunkt() {}

	/**
	 * @param  varde RFC 3339 (2026-09-01T10:15:00+02:00) eller lokal svensk tid (2026-09-01T10:15:00 eller 2026-09-01
	 *               10:15:00)
	 * @return       tidpunkten
	 */
	public static Instant tolka(final String varde) {
		try {
			return OffsetDateTime.parse(varde).toInstant();
		} catch (final DateTimeParseException ignored) {
			// fortsätt med lokala format
		}
		try {
			return LocalDateTime.parse(varde).atZone(ZON).toInstant();
		} catch (final DateTimeParseException ignored) {
			// fortsätt
		}
		return LocalDateTime.parse(varde, LOKAL).atZone(ZON).toInstant();
	}

	public static String formatera(final Instant tidpunkt) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(tidpunkt.atZone(ZON).toOffsetDateTime());
	}

	/**
	 * @return datumet kl. 00:00 svensk tid, eller null
	 */
	public static Instant dagensStart(final String datum) {
		return datum == null ? null : LocalDate.parse(datum).atStartOfDay(ZON).toInstant();
	}

	/**
	 * @return datumet kl. 23:59:59.999 svensk tid, eller null
	 */
	public static Instant dagensSlut(final String datum) {
		return datum == null ? null : LocalDate.parse(datum).plusDays(1).atStartOfDay(ZON).toInstant().minusMillis(1);
	}
}
