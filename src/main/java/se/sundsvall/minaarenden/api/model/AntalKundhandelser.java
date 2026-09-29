package se.sundsvall.minaarenden.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * @param antal antal kundhändelser
 */
@Schema(description = "Antal kundhändelser", accessMode = READ_ONLY)
public record AntalKundhandelser(@Schema(examples = "5") long antal) {
}
