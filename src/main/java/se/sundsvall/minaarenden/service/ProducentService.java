package se.sundsvall.minaarenden.service;

import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.minaarenden.configuration.MinaArendenProperties;
import se.sundsvall.minaarenden.configuration.MinaArendenProperties.Producent;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProducentService {

	private final MinaArendenProperties properties;

	public ProducentService(final MinaArendenProperties properties) {
		this.properties = properties;
	}

	/**
	 * @param  municipalityId                               kommun-id ur sökvägen
	 * @return                                              producenten för kommunen
	 * @throws se.sundsvall.dept44.problem.ThrowableProblem 404 om kommunen inte är konfigurerad
	 */
	public Producent hamta(final String municipalityId) {
		return properties.producent(municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Kommun " + municipalityId + " är inte konfigurerad som producent"));
	}

	public String standardVersion() {
		return properties.standardVersion();
	}
}
