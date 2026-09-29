package se.sundsvall.minaarenden.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;

/**
 * Producentkonfiguration per kommun. Varje kommun som ska svara på frågor eller ta emot kundhändelser måste finnas i
 * listan producenter; anrop för andra kommun-id:n ger 404.
 *
 * @param standardVersion version av Mina ärendens standard som producenten implementerar, t.ex. 6.1
 * @param seed            läs in exempelhändelser vid start för de kommuner som saknar kundhändelser (endast
 *                        utveckling/demo)
 * @param producenter     producenter, en per kommun
 */
@Validated
@ConfigurationProperties(prefix = "minaarenden")
public record MinaArendenProperties(
	@NotBlank @Pattern(regexp = "^\\d+(?:\\.\\d+)*$") @DefaultValue("6.1") String standardVersion,
	@DefaultValue("false") boolean seed,
	@NotEmpty List<@Valid Producent> producenter) {

	public MinaArendenProperties {
		producenter = Optional.ofNullable(producenter).map(List::copyOf).orElse(List.of());
		final var kommuner = new HashSet<String>();
		producenter.stream()
			.map(Producent::municipalityId)
			.filter(id -> !kommuner.add(id))
			.findFirst()
			.ifPresent(id -> {
				throw new IllegalStateException("Kommun-id " + id + " förekommer flera gånger i minaarenden.producenter");
			});
	}

	/**
	 * @param  municipalityId kommun-id, t.ex. 2281
	 * @return                producenten för kommunen, om den är konfigurerad
	 */
	public Optional<Producent> producent(final String municipalityId) {
		return producenter.stream()
			.filter(producent -> producent.municipalityId().equals(municipalityId))
			.findFirst();
	}

	/**
	 * @param municipalityId kommun-id (fyra siffror), första delen i sökvägen
	 * @param namn           producentens namn i svaren (delfragor.producent och kundhandelse.producent)
	 * @param prefix         producentprefix, första delen i kundhändelsetyper (versaler A-Z)
	 */
	public record Producent(
		@ValidMunicipalityId String municipalityId,
		@NotBlank String namn,
		@NotBlank @Pattern(regexp = "^[A-Z]+$", message = "ska bestå av versaler A-Z, t.ex. REFKOM") String prefix) {
	}
}
