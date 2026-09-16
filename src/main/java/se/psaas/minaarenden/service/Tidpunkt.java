package se.psaas.minaarenden.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Tidpunkter enligt RFC 3339 i svar; toleranta vid inläsning. Datumgränser tolkas i svensk tid. */
public final class Tidpunkt {

    public static final ZoneId ZON = ZoneId.of("Europe/Stockholm");
    private static final DateTimeFormatter LOKAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Tidpunkt() {}

    public static Instant parse(String s) {
        try {
            return OffsetDateTime.parse(s).toInstant();
        } catch (DateTimeParseException ignored) {
            // fortsätt med lokala format
        }
        try {
            return LocalDateTime.parse(s).atZone(ZON).toInstant();
        } catch (DateTimeParseException ignored) {
            // fortsätt
        }
        return LocalDateTime.parse(s, LOKAL).atZone(ZON).toInstant();
    }

    public static String format(Instant i) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(i.atZone(ZON).toOffsetDateTime());
    }

    /** Startdatum kl. 00:00. */
    public static Instant dagStart(String datum) {
        return datum == null ? null : LocalDate.parse(datum).atStartOfDay(ZON).toInstant();
    }

    /** Slutdatum kl. 23:59:59.999. */
    public static Instant dagSlut(String datum) {
        return datum == null ? null : LocalDate.parse(datum).plusDays(1).atStartOfDay(ZON).toInstant().minusMillis(1);
    }
}
