package se.psaas.minaarenden.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Vem frågan gäller")
public record Kund(
        @Schema(description = "Ett värde som unikt identifierar kunden, 12 siffror", example = "199009090000")
        @NotBlank @Pattern(regexp = "^\\d{12}$", message = "kund.identifierare ska vara 12 siffror")
        String identifierare,
        @Schema(description = "Löpnummer som skiljer flera enskilda firmor åt", nullable = true)
        String tillagg,
        @NotBlank @Pattern(regexp = "^(Personnummer|Organisationsnummer|Samordningsnummer)$", message = "kund.typ har ogiltigt värde")
        String typ) {
}
