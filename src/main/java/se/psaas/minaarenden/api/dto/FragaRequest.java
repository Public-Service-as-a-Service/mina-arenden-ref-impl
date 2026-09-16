package se.psaas.minaarenden.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(name = "KundhandelseFragaRequest")
public record FragaRequest(
        @NotNull @Valid Fraga fraga,
        @Schema(description = "Personnummer för användaren som begär uppgifterna", example = "194903012658")
        @NotBlank @Pattern(regexp = "^\\d{12}$", message = "anvandare ska vara 12 siffror") String anvandare) {
}
