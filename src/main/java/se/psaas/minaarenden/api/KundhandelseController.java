package se.psaas.minaarenden.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import se.psaas.minaarenden.api.dto.CachadKundhandelse;
import se.psaas.minaarenden.api.dto.Granser;
import se.psaas.minaarenden.api.dto.NyKundhandelse;
import se.psaas.minaarenden.config.OpenApiConfig;
import se.psaas.minaarenden.service.KundhandelseService;

/**
 * Inläsning till ärendecachen. Verksamhetssystemet (eller en integrationsplattform) skickar in
 * kundhändelser här när något händer i ett ärende. Skyddas med X-Api-Key om ADMIN_API_KEY är satt.
 */
@RestController
@Tag(name = "Ärendecache", description = "Läs in, hämta och ta bort kundhändelser i cachen")
@SecurityRequirement(name = OpenApiConfig.API_KEY)
public class KundhandelseController {

    private final KundhandelseService service;

    public KundhandelseController(KundhandelseService service) {
        this.service = service;
    }

    public record Sparad(int antal) {}

    @Operation(summary = "Lägg till eller ersätt kundhändelser",
            description = "Skicka en lista med högst " + Granser.MAX_INLASNING + " kundhändelser. Befintlig kundhändelse med samma kundhandelseId ersätts. "
                    + "Listan sparas i en transaktion; vid 409 (samtidig inläsning av samma nya kundhandelseId) kan anropet skickas om oförändrat.")
    @PostMapping(value = "/kundhandelser", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Sparad spara(
            @RequestBody
            @NotEmpty(message = "listan får inte vara tom")
            @Size(max = Granser.MAX_INLASNING, message = "högst " + Granser.MAX_INLASNING + " kundhändelser per anrop")
            List<@Valid NyKundhandelse> nya) {
        return new Sparad(service.spara(nya));
    }

    @Operation(summary = "Hämta en kundhändelse ur cachen")
    @GetMapping(value = "/kundhandelser/{kundhandelseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CachadKundhandelse hamta(@PathVariable String kundhandelseId) {
        return service.hamta(kundhandelseId);
    }

    @Operation(summary = "Ta bort en kundhändelse ur cachen")
    @DeleteMapping("/kundhandelser/{kundhandelseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void taBort(@PathVariable String kundhandelseId) {
        service.taBort(kundhandelseId);
    }

    @Operation(summary = "Antal kundhändelser i cachen")
    @GetMapping(value = "/kundhandelser", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Long> antal() {
        return Map.of("antal", service.antal());
    }
}
