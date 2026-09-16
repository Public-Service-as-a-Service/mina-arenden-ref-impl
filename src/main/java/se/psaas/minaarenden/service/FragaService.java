package se.psaas.minaarenden.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.psaas.minaarenden.api.dto.Behandling;
import se.psaas.minaarenden.api.dto.Delfraga;
import se.psaas.minaarenden.api.dto.Fraga;
import se.psaas.minaarenden.api.dto.FragaRequest;
import se.psaas.minaarenden.api.dto.FragaResponse;
import se.psaas.minaarenden.api.dto.KundhandelserForPart;
import se.psaas.minaarenden.api.dto.Metadata;
import se.psaas.minaarenden.api.dto.Paginering;
import se.psaas.minaarenden.api.dto.Part;
import se.psaas.minaarenden.api.dto.Sortering;
import se.psaas.minaarenden.config.MinaArendenProperties;
import se.psaas.minaarenden.domain.KundhandelseEntity;
import se.psaas.minaarenden.domain.KundhandelseRepository;
import se.psaas.minaarenden.domain.KundhandelseSpecifications;
import se.psaas.minaarenden.domain.OffsetPageable;

/**
 * Besvarar en synkron fråga ur ärendecachen. Som ensam producent svarar tjänsten alltid med 200 och
 * en delfråga per part med status OK; vidareförmedlingstjänsten sätter 206 om någon producent fallerar.
 *
 * <p>Filtrering, sortering och paginering görs i databasen. Bara den begärda sidan läses in i minnet;
 * totaltAntalKundhandelser hämtas med en separat COUNT-fråga när det behövs.
 */
@Service
public class FragaService {

    private final KundhandelseRepository repository;
    private final KundhandelseMapper mapper;
    private final MinaArendenProperties properties;

    public FragaService(KundhandelseRepository repository, KundhandelseMapper mapper, MinaArendenProperties properties) {
        this.repository = repository;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public FragaResponse besvara(FragaRequest request, String onskatSprak) {
        Fraga fraga = request.fraga();
        validera(fraga);
        Behandling behandling = fraga.behandling();
        List<Sortering> sortering = behandling == null ? null : behandling.sortering();
        Paginering paginering = behandling == null ? null : behandling.paginering();

        List<KundhandelserForPart> perPart = new ArrayList<>();
        List<Delfraga> delfragor = new ArrayList<>();
        for (Part part : fraga.parter()) {
            Specification<KundhandelseEntity> spec = KundhandelseSpecifications.forPart(part.kund(), part.arende())
                    .and(KundhandelseSpecifications.kundhandelseTyper(fraga.kundhandelseTyper()))
                    .and(KundhandelseSpecifications.taggar(fraga.taggar()))
                    .and(KundhandelseSpecifications.tidpunktFrom(Tidpunkt.dagStart(fraga.startDatum())))
                    .and(KundhandelseSpecifications.tidpunktTo(Tidpunkt.dagSlut(fraga.slutDatum())))
                    .and(KundhandelseSpecifications.sorterad(sortering));

            List<KundhandelseEntity> traffar;
            long totalt;
            if (paginering == null) {
                traffar = repository.findAll(spec);
                totalt = traffar.size();
            } else {
                // Utan limit ska alla kundhändelser från offset till slutet lämnas (spec).
                int limit = paginering.limit() == null ? Integer.MAX_VALUE : paginering.limit();
                Page<KundhandelseEntity> sida = repository.findAll(spec, new OffsetPageable(paginering.offset(), limit));
                traffar = sida.getContent();
                totalt = sida.getTotalElements();
            }
            perPart.add(new KundhandelserForPart(part, totalt, traffar.stream().map(mapper::toDto).toList()));
            delfragor.add(new Delfraga(properties.producent(), part, "OK", null, 200));
        }

        Metadata metadata = new Metadata(fraga.parter(), fraga.kundhandelseTyper(), fraga.taggar(), fraga.startDatum(), fraga.slutDatum(),
                onskatSprak, fraga.behandling());
        return new FragaResponse(perPart, metadata, delfragor);
    }

    private static void validera(Fraga fraga) {
        for (Part p : fraga.parter()) {
            if (p == null || p.isEmpty()) {
                throw new OgiltigFragaException("Varje part måste innehålla kund, ärende eller båda");
            }
        }
        Behandling b = fraga.behandling();
        if (b != null && b.paginering() != null && (b.sortering() == null || b.sortering().isEmpty())) {
            throw new OgiltigFragaException("Paginering kräver att sortering anges");
        }
        if (fraga.startDatum() != null && fraga.slutDatum() != null && fraga.startDatum().compareTo(fraga.slutDatum()) > 0) {
            throw new OgiltigFragaException("startDatum får inte vara efter slutDatum");
        }
    }
}
