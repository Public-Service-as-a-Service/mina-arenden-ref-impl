package se.sundsvall.minaarenden.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.minaarenden.api.model.Behandling;
import se.sundsvall.minaarenden.api.model.Delfraga;
import se.sundsvall.minaarenden.api.model.Fraga;
import se.sundsvall.minaarenden.api.model.KundhandelseFragaRequest;
import se.sundsvall.minaarenden.api.model.KundhandelseFragaResponse;
import se.sundsvall.minaarenden.api.model.KundhandelserForPart;
import se.sundsvall.minaarenden.api.model.Metadata;
import se.sundsvall.minaarenden.api.model.Paginering;
import se.sundsvall.minaarenden.integration.db.KundhandelseRepository;
import se.sundsvall.minaarenden.integration.db.OffsetPageable;
import se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity;
import se.sundsvall.minaarenden.service.mapper.KundhandelseMapper;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.forPart;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.sorteradEfter;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.withKundhandelseTyper;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.withMunicipalityId;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.withNagonAvTaggarna;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.withTidpunktFrom;
import static se.sundsvall.minaarenden.integration.db.specification.KundhandelseSpecification.withTidpunktTom;

/**
 * Besvarar en synkron fråga ur ärendecachen. Som ensam producent svarar tjänsten alltid med 200 och en delfråga per
 * part med status OK; vidareförmedlingstjänsten sätter 206 om någon producent fallerar.
 *
 * <p>
 * Filtrering, sortering och paginering görs i databasen. Bara den begärda sidan läses in i minnet;
 * totaltAntalKundhandelser hämtas med en separat COUNT-fråga när det behövs.
 */
@Service
public class FragaService {

	static final String STATUS_OK = "OK";

	private final KundhandelseRepository repository;
	private final ProducentService producentService;

	public FragaService(final KundhandelseRepository repository, final ProducentService producentService) {
		this.repository = repository;
		this.producentService = producentService;
	}

	@Transactional(readOnly = true)
	public KundhandelseFragaResponse besvara(final String municipalityId, final KundhandelseFragaRequest request, final String onskatSprak) {
		final var producent = producentService.hamta(municipalityId);
		final var fraga = request.getFraga();
		validera(fraga);

		final var behandling = Optional.ofNullable(fraga.getBehandling());
		final var sortering = behandling.map(Behandling::getSortering).orElse(null);
		final var paginering = behandling.map(Behandling::getPaginering).orElse(null);
		final var startDatum = Tidpunkt.dagensStart(fraga.getStartDatum());
		final var slutDatum = Tidpunkt.dagensSlut(fraga.getSlutDatum());

		final var perPart = new ArrayList<KundhandelserForPart>();
		final var delfragor = new ArrayList<Delfraga>();
		for (final var part : fraga.getParter()) {
			final var specification = Specification.allOf(
				withMunicipalityId(municipalityId),
				forPart(part.getKund(), part.getArende()),
				withKundhandelseTyper(fraga.getKundhandelseTyper()),
				withNagonAvTaggarna(fraga.getTaggar()),
				withTidpunktFrom(startDatum),
				withTidpunktTom(slutDatum),
				sorteradEfter(sortering));

			perPart.add(sok(specification, paginering).withPart(part));
			delfragor.add(Delfraga.create()
				.withProducent(producent.namn())
				.withPart(part)
				.withStatus(STATUS_OK)
				.withHttpCode(200));
		}

		return KundhandelseFragaResponse.create()
			.withKundhandelser(perPart)
			.withMetadata(Metadata.create()
				.withParter(fraga.getParter())
				.withKundhandelseTyper(fraga.getKundhandelseTyper())
				.withTaggar(fraga.getTaggar())
				.withStartDatum(fraga.getStartDatum())
				.withSlutDatum(fraga.getSlutDatum())
				.withOnskatSprak(onskatSprak)
				.withBehandling(fraga.getBehandling()))
			.withDelfragor(delfragor);
	}

	private KundhandelserForPart sok(final Specification<KundhandelseEntity> specification, final Paginering paginering) {
		final List<KundhandelseEntity> traffar;
		final long totalt;
		if (paginering == null) {
			traffar = repository.findAll(specification);
			totalt = traffar.size();
		} else {
			// Utan limit ska alla kundhändelser från offset till slutet lämnas (spec).
			final var limit = Optional.ofNullable(paginering.getLimit()).orElse(Integer.MAX_VALUE);
			final var sida = repository.findAll(specification, new OffsetPageable(paginering.getOffset(), limit));
			traffar = sida.getContent();
			totalt = sida.getTotalElements();
		}
		return KundhandelserForPart.create()
			.withTotaltAntalKundhandelser(totalt)
			.withKundhandelserForPart(traffar.stream().map(KundhandelseMapper::toKundhandelse).toList());
	}

	private static void validera(final Fraga fraga) {
		if (fraga.getParter().stream().anyMatch(part -> part == null || part.isEmpty())) {
			throw Problem.valueOf(BAD_REQUEST, "Varje part måste innehålla kund, ärende eller båda");
		}
		final var behandling = fraga.getBehandling();
		if (behandling != null && behandling.getPaginering() != null && (behandling.getSortering() == null || behandling.getSortering().isEmpty())) {
			throw Problem.valueOf(BAD_REQUEST, "Paginering kräver att sortering anges");
		}
		if (fraga.getStartDatum() != null && fraga.getSlutDatum() != null && fraga.getStartDatum().compareTo(fraga.getSlutDatum()) > 0) {
			throw Problem.valueOf(BAD_REQUEST, "startDatum får inte vara efter slutDatum");
		}
	}

}
