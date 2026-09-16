package se.psaas.minaarenden.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Inläsning till ärendecachen: en kundhändelse och den part den gäller. Längdgränserna motsvarar
 * kolumnerna i tabellen kundhandelse, så att fel ger 400 med tydligt fält i stället för databasfel.
 */
@Schema(description = "Kundhändelse att lägga i ärendecachen. producent, sprak och version fylls i från konfigurationen om de utelämnas.")
public record NyKundhandelse(
        @NotBlank @Size(max = 100) String kundhandelseId,
        @NotNull @Valid Part part,
        @NotBlank @Size(max = 255) String rubrik,
        @NotBlank @Size(max = 65535) String beskrivning,
        @Schema(description = "ISO 639-1, t.ex. sv", example = "sv")
        @Pattern(regexp = "^[a-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$", message = "sprak ska vara en språkkod enligt ISO 639-1, t.ex. sv")
        @Size(max = 10) String sprak,
        @Schema(description = "RFC 3339, eller lokal tid ÅÅÅÅ-MM-DD HH:MM:SS i Europe/Stockholm", example = "2026-09-01T10:15:00+02:00")
        @NotBlank @Size(max = 40) String tidpunkt,
        @NotBlank @Size(max = Kundhandelsetyp.MAX_LANGD)
        @Pattern(regexp = Kundhandelsetyp.INLASNING_MONSTER, message = "kundhandelseTyp ska ha formatet PRODUCENT.ARENDETYP.HANDELSETYP (versaler, siffror, understreck)")
        String kundhandelseTyp,
        @NotNull Boolean producentarendetKraverKundatgard,
        @NotNull Boolean producentarendetKlart,
        @Schema(description = "Version av standarden, t.ex. 6.1", example = "6.1")
        @Pattern(regexp = "^\\d+(?:\\.\\d+)*$", message = "version ska anges som t.ex. 6.1")
        @Size(max = 10) String version,
        JsonNode utokadInformation,
        JsonNode referenser,
        @Size(max = 100, message = "taggar får innehålla högst 100 taggar")
        List<@NotBlank(message = "taggar får inte innehålla tomma värden") @Size(max = 100, message = "taggar: för lång tagg") String> taggar) {
}
