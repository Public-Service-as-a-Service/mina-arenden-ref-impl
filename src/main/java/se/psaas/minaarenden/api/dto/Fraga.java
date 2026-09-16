package se.psaas.minaarenden.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record Fraga(
        @NotEmpty(message = "fraga.parter måste innehålla minst en part")
        @Size(max = Granser.MAX_PARTER, message = "fraga.parter får innehålla högst " + Granser.MAX_PARTER + " parter")
        @Valid List<Part> parter,
        @Schema(description = "Kundhändelsetyper, t.ex. REFKOM.BYGGLOV eller REFKOM.BYGGLOV.ANSOKAN_MOTTAGEN. Ett prefix matchar alla typer under det.")
        @Size(max = Granser.MAX_KUNDHANDELSETYPER, message = "kundhandelseTyper får innehålla högst " + Granser.MAX_KUNDHANDELSETYPER + " typer")
        List<@Pattern(regexp = Kundhandelsetyp.FRAGA_MONSTER, message = "kundhandelseTyper har ogiltigt format")
             @Size(max = Kundhandelsetyp.MAX_LANGD, message = "kundhandelseTyper: för lång typ") String> kundhandelseTyper,
        @Size(max = Granser.MAX_TAGGAR, message = "taggar får innehålla högst " + Granser.MAX_TAGGAR + " taggar")
        List<@Size(max = 100, message = "taggar: för lång tagg") String> taggar,
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "startDatum ska ha formatet ÅÅÅÅ-MM-DD") String startDatum,
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "slutDatum ska ha formatet ÅÅÅÅ-MM-DD") String slutDatum,
        @Valid Behandling behandling) {
}
