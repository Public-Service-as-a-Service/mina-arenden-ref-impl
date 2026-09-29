package se.sundsvall.minaarenden.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.Objects;

/**
 * En part identifieras som kund, ärende eller båda. Anges båda ska svaret uppfylla båda kriterierna.
 */
@Schema(description = "En part identifieras som kund, ärende eller båda")
public class Part {

	@Valid
	private Kund kund;

	@Valid
	private Arende arende;

	public static Part create() {
		return new Part();
	}

	public Kund getKund() {
		return kund;
	}

	public void setKund(final Kund kund) {
		this.kund = kund;
	}

	public Part withKund(final Kund kund) {
		this.kund = kund;
		return this;
	}

	public Arende getArende() {
		return arende;
	}

	public void setArende(final Arende arende) {
		this.arende = arende;
	}

	public Part withArende(final Arende arende) {
		this.arende = arende;
		return this;
	}

	/**
	 * @return true om varken kund eller ärende är angivet
	 */
	@JsonIgnore
	@Schema(hidden = true)
	public boolean isEmpty() {
		return kund == null && arende == null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(kund, arende);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Part other)) {
			return false;
		}
		return Objects.equals(kund, other.kund) && Objects.equals(arende, other.arende);
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("Part [kund=").append(kund)
			.append(", arende=").append(arende)
			.append("]").toString();
	}
}
