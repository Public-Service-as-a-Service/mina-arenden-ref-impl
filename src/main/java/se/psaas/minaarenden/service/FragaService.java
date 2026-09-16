package se.psaas.minaarenden.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.psaas.minaarenden.api.dto.Behandling;
import se.psaas.minaarenden.api.dto.Delfraga;
import se.psaas.minaarenden.api.dto.Fraga;
import se.psaas.minaarenden.api.dto.FragaRequest;
import se.psaas.minaarenden.api.dto.FragaResponse;
import se.psaas.minaarenden.api.dto.Kundhandelse;
import se.psaas.minaarenden.api.dto.KundhandelserForPart;
import se.psaas.minaarenden.api.dto.Metadata;
import se.psaas.minaarenden.api.dto.Part;
import se.psaas.minaarenden.api.dto.Sortering;
import se.psaas.minaarenden.config.MinaArendenProperties;
import se.psaas.minaarenden.domain.KundhandelseEntity;
import se.psaas.minaarenden.domain.KundhandelseRepository;
import se.psaas.minaarenden.domain.KundhandelseSpecifications;

/**
 * Besvarar en synkron fråga ur ärendecachen. Som ensam producent svarar tjänsten alltid med 200 och
 * en delfråga per part med status OK; vidareförmedlingstjänsten sätter 206 om någon producent fallerar.
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

        List<KundhandelserForPart> perPart = new ArrayList<>();
        List<Delfraga> delfragor = new ArrayList<>();
        for (Part part : fraga.parter()) {
            Specification<KundhandelseEntity> spec = KundhandelseSpecifications.forPart(part.kund(), part.arende())
                    .and(KundhandelseSpecifications.kundhandelseTyper(fraga.kundhandelseTyper()))
                    .and(KundhandelseSpecifications.taggar(fraga.taggar()))
                    .and(KundhandelseSpecifications.tidpunktFrom(Tidpunkt.dagStart(fraga.startDatum())))
                    .and(KundhandelseSpecifications.tidpunktTo(Tidpunkt.dagSlut(fraga.slutDatum())));
            List<Kundhandelse> alla = repository.findAll(spec).stream().map(mapper::toDto).toList();
            List<Kundhandelse> behandlade = behandla(alla, fraga.behandling());
            perPart.add(new KundhandelserForPart(part, alla.size(), behandlade));
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

    /** Sortering och paginering sker hos producenten på samma sätt som i vidareförmedlingstjänsten. */
    static List<Kundhandelse> behandla(List<Kundhandelse> alla, Behandling behandling) {
        if (behandling == null) {
            return alla.stream().sorted(Comparator.comparing(Kundhandelse::tidpunkt).reversed()).toList();
        }
        List<Kundhandelse> list = new ArrayList<>(alla);
        if (behandling.sortering() != null && !behandling.sortering().isEmpty()) {
            Comparator<Kundhandelse> cmp = null;
            for (Sortering s : behandling.sortering()) {
                Comparator<Kundhandelse> c = comparator(s.attribut());
                if (!Boolean.TRUE.equals(s.stigande())) {
                    c = c.reversed();
                }
                cmp = cmp == null ? c : cmp.thenComparing(c);
            }
            list.sort(cmp);
        }
        if (behandling.paginering() != null) {
            int from = Math.min(behandling.paginering().offset(), list.size());
            int to = behandling.paginering().limit() == null ? list.size() : Math.min(from + behandling.paginering().limit(), list.size());
            list = list.subList(from, to);
        }
        return list;
    }

    private static Comparator<Kundhandelse> comparator(String attribut) {
        return switch (attribut) {
            case "RUBRIK" -> Comparator.comparing(Kundhandelse::rubrik, String.CASE_INSENSITIVE_ORDER);
            case "BESKRIVNING" -> Comparator.comparing(Kundhandelse::beskrivning, String.CASE_INSENSITIVE_ORDER);
            case "PRODUCENT" -> Comparator.comparing(Kundhandelse::producent, String.CASE_INSENSITIVE_ORDER);
            case "TIDPUNKT" -> Comparator.comparing(Kundhandelse::tidpunkt);
            case "KUNDHANDELSETYP" -> Comparator.comparing(Kundhandelse::kundhandelseTyp);
            case "PRODUCENTARENDETKRAVERKUNDATGARD" -> Comparator.comparing(Kundhandelse::producentarendetKraverKundatgard);
            case "PRODUCENTARENDETKLART" -> Comparator.comparing(Kundhandelse::producentarendetKlart);
            default -> throw new OgiltigFragaException("Okänt sorteringsattribut: " + attribut);
        };
    }
}
