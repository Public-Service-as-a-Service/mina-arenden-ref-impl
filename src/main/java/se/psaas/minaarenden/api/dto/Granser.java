package se.psaas.minaarenden.api.dto;

/**
 * Hårda gränser för inkommande anrop. Standarden anger inga maxvärden; dessa är satta för att
 * begränsa resursåtgången per anrop och ligger långt över realistiska volymer (en fråga från
 * vidareförmedlingstjänsten gäller normalt en eller två parter). Höj vid behov.
 */
public final class Granser {

    private Granser() {}

    /** Antal parter per fråga. Varje part blir en egen databasfråga. */
    public static final int MAX_PARTER = 100;

    /** Antal kundhändelsetyper i ett filter. */
    public static final int MAX_KUNDHANDELSETYPER = 100;

    /** Antal taggar i ett filter. */
    public static final int MAX_TAGGAR = 100;

    /** Största tillåtna paginering.limit. */
    public static final int MAX_LIMIT = 1000;

    /** Antal kundhändelser per inläsning till cachen (POST /kundhandelser). */
    public static final int MAX_INLASNING = 1000;

    /** Längd på skv_client_correlation_id. */
    public static final int MAX_CORRELATION_ID_LANGD = 200;
}
