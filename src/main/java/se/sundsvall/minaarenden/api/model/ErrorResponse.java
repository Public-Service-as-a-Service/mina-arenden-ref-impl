package se.sundsvall.minaarenden.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * Felsvar enligt Skatteverkets API-definition: ett enda fält message (schemat tillåter inga andra fält,
 * därför inte RFC 9457 som övriga gränssnitt i tjänsten).
 */
@Schema(name = "ErrorResponse", description = "Felsvar för /kundhandelseFragaSynkron")
public class ErrorResponse {

	@Schema(examples = "Bad request: fraga.parter måste innehålla minst en part")
	private String message;

	public static ErrorResponse create() {
		return new ErrorResponse();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public ErrorResponse withMessage(final String message) {
		this.message = message;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(message);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ErrorResponse other)) {
			return false;
		}
		return Objects.equals(message, other.message);
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append("ErrorResponse [message=").append(message)
			.append("]").toString();
	}
}
