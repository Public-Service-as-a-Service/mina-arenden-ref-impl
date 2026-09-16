package se.psaas.minaarenden.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;

@Schema(description = "Styr sortering och paginering av kundhändelserna i svaret. Paginering kräver sortering.")
public record Behandling(@Valid List<Sortering> sortering, @Valid Paginering paginering) {
}
