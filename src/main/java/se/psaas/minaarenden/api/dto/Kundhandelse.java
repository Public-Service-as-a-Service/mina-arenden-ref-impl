package se.psaas.minaarenden.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "En kundhändelse enligt Mina ärendens standard")
public record Kundhandelse(
        String kundhandelseId,
        String rubrik,
        String beskrivning,
        @Schema(description = "ISO 639-1", example = "sv") String sprak,
        String producent,
        @Schema(description = "RFC 3339", example = "2026-09-01T10:15:00+02:00") String tidpunkt,
        @Schema(example = "REFKOM.BYGGLOV.ANSOKAN_MOTTAGEN") String kundhandelseTyp,
        boolean producentarendetKraverKundatgard,
        boolean producentarendetKlart,
        @Schema(description = "Version av standarden som producenten implementerat", example = "6.1") String version,
        JsonNode utokadInformation,
        JsonNode referenser) {
}
