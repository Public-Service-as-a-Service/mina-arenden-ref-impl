package se.psaas.minaarenden.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "KundhandelseFragaResponse")
public record FragaResponse(List<KundhandelserForPart> kundhandelser, Metadata metadata, List<Delfraga> delfragor) {
}
