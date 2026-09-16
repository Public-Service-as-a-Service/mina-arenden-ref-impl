package se.psaas.minaarenden.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record Fraga(
        @NotEmpty(message = "fraga.parter måste innehålla minst en part") @Valid List<Part> parter,
        @Schema(description = "Kundhändelsetyper, t.ex. REFKOM.BYGGLOV eller REFKOM.BYGGLOV.ANSOKAN_MOTTAGEN. Ett prefix matchar alla typer under det.")
        List<@Pattern(regexp = "^[A-Z]+(?:\\.[A-Z0-9_]+)*$", message = "kundhandelseTyper har ogiltigt format") String> kundhandelseTyper,
        List<String> taggar,
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "startDatum ska ha formatet ÅÅÅÅ-MM-DD") String startDatum,
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "slutDatum ska ha formatet ÅÅÅÅ-MM-DD") String slutDatum,
        @Valid Behandling behandling) {
}
