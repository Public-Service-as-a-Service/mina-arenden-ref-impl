package se.sundsvall.minaarenden.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.minaarenden.api.model.AntalKundhandelser;
import se.sundsvall.minaarenden.api.model.CachadKundhandelse;
import se.sundsvall.minaarenden.api.model.NyKundhandelse;
import se.sundsvall.minaarenden.api.validation.Granser;
import se.sundsvall.minaarenden.service.KundhandelseService;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

/**
 * Inläsning till ärendecachen. Verksamhetssystemet (eller en integrationsplattform) skickar in kundhändelser här när
 * något händer i ett ärende. Kräver admin-nyckel.
 */
@RestController
@Validated
@Tag(name = "Ärendecache", description = "Läs in, hämta och ta bort kundhändelser i cachen")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "401", description = "API-nyckel saknas eller är ogiltig", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
@ApiResponse(responseCode = "403", description = "API-nyckeln är inte en admin-nyckel", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class KundhandelseResource {

	private final KundhandelseService kundhandelseService;

	KundhandelseResource(final KundhandelseService kundhandelseService) {
		this.kundhandelseService = kundhandelseService;
	}

	@PostMapping(path = ApiPaths.KUNDHANDELSER, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Lägg till eller ersätt kundhändelser",
		description = "Skicka en lista med högst " + Granser.MAX_INLASNING + " kundhändelser. Befintlig kundhändelse med samma kundhandelseId ersätts. "
			+ "Listan sparas i en transaktion; vid 409 (samtidig inläsning av samma nya kundhandelseId) kan anropet skickas om oförändrat.",
		responses = {
			@ApiResponse(responseCode = "201", description = "Sparat", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Samtidig ändring", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<AntalKundhandelser> sparaKundhandelser(
		@Parameter(name = "municipalityId", description = "Kommun-id", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@RequestBody @NotEmpty(message = "listan får inte vara tom") @Size(max = Granser.MAX_INLASNING,
			message = "högst " + Granser.MAX_INLASNING
				+ " kundhändelser per anrop") final List<@Valid NyKundhandelse> kundhandelser) {

		return status(CREATED).body(new AntalKundhandelser(kundhandelseService.spara(municipalityId, kundhandelser)));
	}

	@GetMapping(path = ApiPaths.KUNDHANDELSER + "/{kundhandelseId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Hämta en kundhändelse ur cachen", responses = @ApiResponse(responseCode = "200", description = "Lyckat anrop", useReturnTypeSchema = true))
	ResponseEntity<CachadKundhandelse> hamtaKundhandelse(
		@Parameter(name = "municipalityId", description = "Kommun-id", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@Parameter(name = "kundhandelseId", description = "Kundhändelsens id", example = "REFKOM-BYGG-2026-00123-1") @PathVariable @NotBlank @Size(max = 100) final String kundhandelseId) {

		return ok(kundhandelseService.hamta(municipalityId, kundhandelseId));
	}

	@DeleteMapping(path = ApiPaths.KUNDHANDELSER + "/{kundhandelseId}", produces = ALL_VALUE)
	@Operation(summary = "Ta bort en kundhändelse ur cachen", responses = @ApiResponse(responseCode = "204", description = "Borttagen", useReturnTypeSchema = true))
	ResponseEntity<Void> taBortKundhandelse(
		@Parameter(name = "municipalityId", description = "Kommun-id", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId,
		@Parameter(name = "kundhandelseId", description = "Kundhändelsens id", example = "REFKOM-BYGG-2026-00123-1") @PathVariable @NotBlank @Size(max = 100) final String kundhandelseId) {

		kundhandelseService.taBort(municipalityId, kundhandelseId);
		return noContent().build();
	}

	@GetMapping(path = ApiPaths.KUNDHANDELSER, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Antal kundhändelser i cachen för kommunen", responses = @ApiResponse(responseCode = "200", description = "Lyckat anrop", useReturnTypeSchema = true))
	ResponseEntity<AntalKundhandelser> antalKundhandelser(
		@Parameter(name = "municipalityId", description = "Kommun-id", example = "2281") @PathVariable @ValidMunicipalityId final String municipalityId) {

		return ok(new AntalKundhandelser(kundhandelseService.antal(municipalityId)));
	}
}
