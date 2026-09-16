package se.psaas.minaarenden.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

@Schema(description = "En part identifieras som kund, ärende eller båda")
public record Part(@Valid Kund kund, @Valid Arende arende) {

    public boolean isEmpty() {
        return kund == null && arende == null;
    }
}
