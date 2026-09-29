package se.sundsvall.minaarenden.api.validation;

/**
 * Hårda gränser för inkommande anrop. Standarden anger inga maxvärden; dessa är satta för att begränsa resursåtgången
 * per anrop och ligger långt över realistiska volymer (en fråga från vidareförmedlingstjänsten gäller normalt en eller
 * två parter). Höj vid behov.
 */
public final class Granser {

	/** Antal parter per fråga. Varje part blir en egen databasfråga. */
	public static final int MAX_PARTER = 100;

	/** Antal kundhändelsetyper i ett filter. */
	public static final int MAX_KUNDHANDELSETYPER = 100;

	/** Antal taggar i ett filter eller på en kundhändelse. */
	public static final int MAX_TAGGAR = 100;

	/** Längd på en tagg, motsvarar kolumnen kundhandelse_tagg.tagg. */
	public static final int MAX_TAGG_LANGD = 100;

	/** Största tillåtna paginering.limit. */
	public static final int MAX_LIMIT = 1000;

	/** Antal kundhändelser per inläsning till cachen (POST /{municipalityId}/kundhandelser). */
	public static final int MAX_INLASNING = 1000;

	/** Längd på skv_client_correlation_id. */
	public static final int MAX_CORRELATION_ID_LANGD = 200;

	/**
	 * skv_client_correlation_id: skrivbara ASCII-tecken (inklusive mellanslag), minst ett synligt tecken och begränsad
	 * längd. API-definitionen tillåter valfritt format; tjänstebeskrivningen rekommenderar UUID.
	 */
	public static final String CORRELATION_ID_MONSTER = "^(?=.*[\\x21-\\x7E])[\\x20-\\x7E]{1," + MAX_CORRELATION_ID_LANGD + "}$";

	private Granser() {}
}
