package se.sundsvall.minaarenden.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.minaarenden.api.model.Behandling;
import se.sundsvall.minaarenden.api.model.Paginering;
import se.sundsvall.minaarenden.api.model.Part;
import se.sundsvall.minaarenden.api.model.Sortering;
import se.sundsvall.minaarenden.integration.db.KundhandelseRepository;
import se.sundsvall.minaarenden.integration.db.OffsetPageable;
import se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.minaarenden.service.TestdataFactory.MUNICIPALITY_ID;
import static se.sundsvall.minaarenden.service.TestdataFactory.PRODUCENT;
import static se.sundsvall.minaarenden.service.TestdataFactory.entity;
import static se.sundsvall.minaarenden.service.TestdataFactory.fraga;
import static se.sundsvall.minaarenden.service.TestdataFactory.kund;

@ExtendWith(MockitoExtension.class)
class FragaServiceTest {

	@Mock
	private KundhandelseRepository repository;

	@Mock
	private ProducentService producentService;

	@InjectMocks
	private FragaService service;

	@Test
	@SuppressWarnings("unchecked")
	void besvaraUtanPaginering() {
		final var request = fraga();
		when(producentService.hamta(MUNICIPALITY_ID)).thenReturn(PRODUCENT);
		when(repository.findAll(any(Specification.class))).thenReturn(List.of(entity("TESTKOP-1"), entity("TESTKOP-2")));

		final var svar = service.besvara(MUNICIPALITY_ID, request, "sv");

		assertThat(svar.getKundhandelser()).hasSize(1).first().satisfies(perPart -> {
			assertThat(perPart.getPart()).isEqualTo(request.getFraga().getParter().getFirst());
			assertThat(perPart.getTotaltAntalKundhandelser()).isEqualTo(2);
			assertThat(perPart.getKundhandelserForPart()).extracting(k -> k.getKundhandelseId()).containsExactly("TESTKOP-1", "TESTKOP-2");
		});
		assertThat(svar.getDelfragor()).hasSize(1).first().satisfies(delfraga -> {
			assertThat(delfraga.getProducent()).isEqualTo("Testköpings kommun");
			assertThat(delfraga.getStatus()).isEqualTo("OK");
			assertThat(delfraga.getHttpCode()).isEqualTo(200);
			assertThat(delfraga.getMessage()).isNull();
		});
		assertThat(svar.getMetadata().getOnskatSprak()).isEqualTo("sv");
		assertThat(svar.getMetadata().getParter()).isEqualTo(request.getFraga().getParter());
	}

	@Test
	@SuppressWarnings("unchecked")
	void besvaraMedPagineringUtanLimit() {
		final var request = fraga();
		request.getFraga()
			.withParter(List.of(Part.create().withKund(kund()), Part.create().withKund(kund())))
			.withBehandling(Behandling.create()
				.withSortering(List.of(Sortering.create().withAttribut("TIDPUNKT").withStigande(true)))
				.withPaginering(Paginering.create().withOffset(1)));
		when(producentService.hamta(MUNICIPALITY_ID)).thenReturn(PRODUCENT);
		when(repository.findAll(any(Specification.class), eq(new OffsetPageable(1, Integer.MAX_VALUE))))
			.thenReturn(new PageImpl<>(List.of(entity("TESTKOP-2")), new OffsetPageable(1, Integer.MAX_VALUE), 2));

		final var svar = service.besvara(MUNICIPALITY_ID, request, null);

		assertThat(svar.getKundhandelser()).hasSize(2).allSatisfy(perPart -> {
			assertThat(perPart.getTotaltAntalKundhandelser()).isEqualTo(2);
			assertThat(perPart.getKundhandelserForPart()).hasSize(1);
		});
		assertThat(svar.getDelfragor()).hasSize(2);
	}

	@Test
	@SuppressWarnings("unchecked")
	void besvaraMedLimit() {
		final var request = fraga();
		request.getFraga().withBehandling(Behandling.create()
			.withSortering(List.of(Sortering.create().withAttribut("RUBRIK").withStigande(false)))
			.withPaginering(Paginering.create().withOffset(0).withLimit(5)));
		when(producentService.hamta(MUNICIPALITY_ID)).thenReturn(PRODUCENT);
		when(repository.findAll(any(Specification.class), eq(new OffsetPageable(0, 5))))
			.thenReturn(new PageImpl<KundhandelseEntity>(List.of(), new OffsetPageable(0, 5), 0));

		assertThat(service.besvara(MUNICIPALITY_ID, request, null).getKundhandelser().getFirst().getTotaltAntalKundhandelser()).isZero();
	}

	@Test
	void tomPartAvvisas() {
		final var request = fraga();
		request.getFraga().withParter(List.of(Part.create()));
		when(producentService.hamta(MUNICIPALITY_ID)).thenReturn(PRODUCENT);

		assertBadRequest(request, "Varje part måste innehålla kund, ärende eller båda");
	}

	@Test
	void pagineringUtanSorteringAvvisas() {
		final var request = fraga();
		request.getFraga().withBehandling(Behandling.create().withPaginering(Paginering.create().withOffset(0)));
		when(producentService.hamta(MUNICIPALITY_ID)).thenReturn(PRODUCENT);

		assertBadRequest(request, "Paginering kräver att sortering anges");

		request.getFraga().getBehandling().withSortering(List.of());
		assertBadRequest(request, "Paginering kräver att sortering anges");
	}

	@Test
	void startDatumEfterSlutDatumAvvisas() {
		final var request = fraga();
		request.getFraga().withStartDatum("2026-09-02").withSlutDatum("2026-09-01");
		when(producentService.hamta(MUNICIPALITY_ID)).thenReturn(PRODUCENT);

		assertBadRequest(request, "startDatum får inte vara efter slutDatum");
	}

	@Test
	void okandKommun() {
		when(producentService.hamta("1480")).thenThrow(Problem.valueOf(NOT_FOUND, "saknas"));

		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.besvara("1480", fraga(), null));
		verify(producentService).hamta("1480");
		verifyNoInteractions(repository);
	}

	private void assertBadRequest(final se.sundsvall.minaarenden.api.model.KundhandelseFragaRequest request, final String detalj) {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.besvara(MUNICIPALITY_ID, request, null))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getDetail()).isEqualTo(detalj);
			});
	}
}
