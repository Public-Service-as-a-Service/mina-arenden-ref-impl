package se.sundsvall.minaarenden.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.minaarenden.api.model.NyKundhandelse;
import se.sundsvall.minaarenden.configuration.MinaArendenProperties;
import se.sundsvall.minaarenden.configuration.MinaArendenProperties.Producent;
import se.sundsvall.minaarenden.integration.db.KundhandelseRepository;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeedServiceTest {

	private static final Producent TESTKOP = new Producent("2281", "Testköpings kommun", "TESTKOP");
	private static final Producent TIMRA = new Producent("2262", "Timrå kommun", "TIMRA");

	@Mock
	private KundhandelseService service;

	@Mock
	private KundhandelseRepository repository;

	@Captor
	private ArgumentCaptor<List<NyKundhandelse>> listCaptor;

	@Test
	void exempeldataLasesInBaraForKommunerUtanData() throws Exception {
		when(repository.countByMunicipalityId("2281")).thenReturn(0L);
		when(repository.countByMunicipalityId("2262")).thenReturn(3L);
		when(service.spara(eq("2281"), any())).thenReturn(5);

		seedService(true).run(null);

		verify(service).spara(eq("2281"), listCaptor.capture());
		verify(service, never()).spara(eq("2262"), any());
		assertThat(listCaptor.getValue()).hasSize(5).allSatisfy(ny -> {
			assertThat(ny.getKundhandelseId()).startsWith("TESTKOP-");
			assertThat(ny.getKundhandelseTyp()).startsWith("TESTKOP.");
		});
	}

	@Test
	void utanSeedLasesIngentingIn() throws Exception {
		when(repository.countByMunicipalityId(anyString())).thenReturn(0L);

		seedService(false).run(null);

		verify(repository).countByMunicipalityId("2281");
		verify(repository).countByMunicipalityId("2262");
		verify(service, never()).spara(anyString(), any());
	}

	@Test
	void prefixBytsBaraIIdOchTyp() {
		final var ny = NyKundhandelse.create()
			.withKundhandelseId("REFKOM-BYGG-1")
			.withKundhandelseTyp("REFKOM.BYGGLOV.BESLUT")
			.withRubrik("REFKOM nämns i rubriken");

		SeedService.bytPrefix(ny, "TESTKOP");

		assertThat(ny.getKundhandelseId()).isEqualTo("TESTKOP-BYGG-1");
		assertThat(ny.getKundhandelseTyp()).isEqualTo("TESTKOP.BYGGLOV.BESLUT");
		assertThat(ny.getRubrik()).isEqualTo("REFKOM nämns i rubriken");
		assertThat(SeedService.bytPrefix(NyKundhandelse.create(), "X").getKundhandelseId()).isNull();
	}

	private SeedService seedService(final boolean seed) {
		return new SeedService(new MinaArendenProperties("6.1", seed, List.of(TESTKOP, TIMRA)), service, repository, JsonMapper.shared());
	}
}
