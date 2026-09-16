package se.psaas.minaarenden.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/** Inläsning till ärendecachen: en kundhändelse och den part den gäller. */
@Schema(description = "Kundhändelse att lägga i ärendecachen. producent, sprak och version fylls i från konfigurationen om de utelämnas.")
public record NyKundhandelse(
        @NotBlank String kundhandelseId,
        @NotNull @Valid Part part,
        @NotBlank String rubrik,
        @NotBlank String beskrivning,
        String sprak,
        @Schema(description = "RFC 3339, eller lokal tid ÅÅÅÅ-MM-DD HH:MM:SS i Europe/Stockholm", example = "2026-09-01T10:15:00+02:00")
        @NotBlank String tidpunkt,
        @NotBlank @Pattern(regexp = "^[A-Z]+\\.[A-Z0-9_.]+$", message = "kundhandelseTyp ska ha formatet PRODUCENT.ARENDETYP.HANDELSETYP") String kundhandelseTyp,
        @NotNull Boolean producentarendetKraverKundatgard,
        @NotNull Boolean producentarendetKlart,
        String version,
        JsonNode utokadInformation,
        JsonNode referenser,
        List<String> taggar) {
}
