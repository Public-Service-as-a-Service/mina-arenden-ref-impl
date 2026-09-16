package se.psaas.minaarenden.api.dto;

import java.util.List;

/** En kundhändelse som den ligger i cachen: part, händelse och taggar. */
public record CachadKundhandelse(Part part, Kundhandelse kundhandelse, List<String> taggar) {
}
