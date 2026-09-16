package se.psaas.minaarenden.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import se.psaas.minaarenden.api.dto.FragaRequest;
import se.psaas.minaarenden.api.dto.FragaResponse;
import se.psaas.minaarenden.service.FragaService;

/**
 * Producentens gränssnitt mot vidareförmedlingstjänsten. Samma operation, request och response som
 * i Skatteverkets API-definition för Mina ärenden kundhändelser.
 */
@RestController
@Tag(name = "Kundhändelser", description = "Fråga om kundhändelser (Mina ärendens standard)")
public class FragaController {

    private static final Logger log = LoggerFactory.getLogger(FragaController.class);

    private final FragaService service;

    public FragaController(FragaService service) {
        this.service = service;
    }

    @Operation(
            summary = "Hämta kundhändelser för en eller flera parter (synkron fråga)",
            description = "Besvarar frågan ur ärendecachen. Parter kan anges som kund, ärende eller båda. "
                    + "Filtrera med kundhandelseTyper (prefix matchar underliggande typer), taggar och datumintervall. "
                    + "Sortering och paginering styrs med behandling; paginering kräver sortering.")
    @ApiResponse(responseCode = "200", description = "Lyckat anrop")
    @ApiResponse(responseCode = "400", description = "Frågan är inte korrekt ställd")
    @ApiResponse(responseCode = "401", description = "client_id eller client_secret saknas eller är fel")
    @PostMapping(value = "/kundhandelseFragaSynkron", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FragaResponse fraga(
            @Parameter(in = ParameterIn.HEADER, required = true, description = "Unikt id per anrop (UUID enligt RFC 4122)", example = "0002aa29-49f2-4baf-be51-c7c39c9824b4")
            @RequestHeader("skv_client_correlation_id") String correlationId,
            @Parameter(in = ParameterIn.HEADER, description = "Önskat språk", example = "sv")
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @Valid @RequestBody FragaRequest request) {
        if (correlationId.isBlank()) {
            throw new se.psaas.minaarenden.service.OgiltigFragaException("skv_client_correlation_id får inte vara tom");
        }
        log.info("Fråga {} från användare {} om {} part(er)", correlationId, mask(request.anvandare()), request.fraga().parter().size());
        return service.besvara(request, acceptLanguage);
    }

    private static String mask(String personnummer) {
        return personnummer == null || personnummer.length() < 8 ? "?" : personnummer.substring(0, 8) + "****";
    }
}
