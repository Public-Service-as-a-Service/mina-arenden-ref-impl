package se.sundsvall.minaarenden.configuration;

/**
 * Roller som en API-nyckel ger.
 */
public final class Roller {

	/** Får ställa frågor (vidareförmedlingstjänsten). */
	public static final String FRAGA = "FRAGA";

	/** Får allt, även underhålla ärendecachen och läsa driftinformation. */
	public static final String ADMIN = "ADMIN";

	private Roller() {}
}
