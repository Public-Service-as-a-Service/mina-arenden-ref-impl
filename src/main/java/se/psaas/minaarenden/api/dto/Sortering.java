package se.psaas.minaarenden.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record Sortering(
        @NotNull @Pattern(regexp = "^(RUBRIK|BESKRIVNING|PRODUCENT|TIDPUNKT|KUNDHANDELSETYP|PRODUCENTARENDETKRAVERKUNDATGARD|PRODUCENTARENDETKLART)$", message = "sortering.attribut har ogiltigt värde")
        String attribut,
        @NotNull Boolean stigande) {
}
