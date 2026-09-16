package se.psaas.minaarenden.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Används för att smalna av frågan till ett specifikt ärende")
public record Arende(
        @Schema(description = "Ett värde som unikt identifierar ärendet", example = "BYGG-2026-00123")
        @NotBlank @Size(max = 100, message = "arende.identifierare får vara högst 100 tecken") String identifierare,
        @NotBlank @Pattern(regexp = "^(Konsumentreferens|Producentarendereferens|Diarienummer|Kvittensnummer)$", message = "arende.typ har ogiltigt värde")
        String typ) {
}
