package se.psaas.minaarenden.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Används för att smalna av frågan till ett specifikt ärende")
public record Arende(
        @Schema(description = "Ett värde som unikt identifierar ärendet", example = "BYGG-2026-00123")
        @NotBlank String identifierare,
        @NotBlank @Pattern(regexp = "^(Konsumentreferens|Producentarendereferens|Diarienummer|Kvittensnummer)$", message = "arende.typ har ogiltigt värde")
        String typ) {
}
