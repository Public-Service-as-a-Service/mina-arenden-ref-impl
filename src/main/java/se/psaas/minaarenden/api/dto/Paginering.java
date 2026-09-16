package se.psaas.minaarenden.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record Paginering(
        @NotNull @Min(0) Integer offset,
        @Min(1) Integer limit) {
}
