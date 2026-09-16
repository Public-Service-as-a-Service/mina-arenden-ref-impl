package se.psaas.minaarenden.api.dto;

import java.util.List;

public record KundhandelserForPart(Part part, long totaltAntalKundhandelser, List<Kundhandelse> kundhandelserForPart) {
}
