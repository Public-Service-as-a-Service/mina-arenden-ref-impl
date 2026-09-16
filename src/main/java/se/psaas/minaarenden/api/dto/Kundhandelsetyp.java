package se.psaas.minaarenden.api.dto;

/**
 * Format för kundhändelsetyper: PRODUCENT.PRODUCENTARENDETYP.KUNDHANDELSETYP i versaler, siffror och
 * understreck, utan å, ä, ö (Mina ärendens standard).
 */
public final class Kundhandelsetyp {

    private Kundhandelsetyp() {}

    /**
     * Vid fråga: samma mönster som JSON-schemat i Skatteverkets API-definition
     * ({@code ^[A-Z]+\.(?:[A-Z0-9_.]+)+$}) samt ett ensamt producentprefix (t.ex. {@code REFKOM}),
     * som i schemats exempel. Producenten ska inte avvisa frågor som vidareförmedlingstjänsten släpper igenom.
     */
    public static final String FRAGA_MONSTER = "^[A-Z]+(?:\\.[A-Z0-9_.]+)?$";

    /**
     * Vid inläsning till cachen: strikt, minst tre delar (producent, producentärendetyp, kundhändelsetyp)
     * och inga tomma delar. Producentens egna data ska följa standardens namngivning fullt ut.
     */
    public static final String INLASNING_MONSTER = "^[A-Z]+(?:\\.[A-Z0-9_]+){2,}$";

    /** Maxlängd, motsvarar kolumnen kundhandelse_typ. */
    public static final int MAX_LANGD = 200;
}
