package se.psaas.minaarenden.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.psaas.minaarenden.api.dto.CachadKundhandelse;
import se.psaas.minaarenden.api.dto.NyKundhandelse;
import se.psaas.minaarenden.config.MinaArendenProperties;
import se.psaas.minaarenden.domain.KundhandelseEntity;
import se.psaas.minaarenden.domain.KundhandelseRepository;

/** Underhåll av ärendecachen: verksamhetssystemet skickar in kundhändelser, cachen svarar på frågor. */
@Service
public class KundhandelseService {

    private final KundhandelseRepository repository;
    private final KundhandelseMapper mapper;
    private final MinaArendenProperties properties;

    public KundhandelseService(KundhandelseRepository repository, KundhandelseMapper mapper, MinaArendenProperties properties) {
        this.repository = repository;
        this.mapper = mapper;
        this.properties = properties;
    }

    /**
     * Skapar eller ersätter (samma kundhandelseId) kundhändelser. Returnerar antal sparade.
     *
     * <p>Hela listan sparas i en transaktion. Om två anrop samtidigt skapar samma nya kundhandelseId
     * skyddar det unika databasvillkoret mot dubbletter; det anrop som förlorar får 409 och kan
     * skickas om oförändrat, eftersom operationen är idempotent.
     */
    @Transactional
    public int spara(List<NyKundhandelse> nya) {
        String prefix = properties.prefix() + ".";
        int antal = 0;
        for (NyKundhandelse n : nya) {
            if (n.part().isEmpty()) {
                throw new OgiltigFragaException("part måste innehålla kund, ärende eller båda (" + n.kundhandelseId() + ")");
            }
            if (!n.kundhandelseTyp().startsWith(prefix)) {
                throw new OgiltigFragaException("kundhandelseTyp måste börja med producentprefixet " + prefix + " (" + n.kundhandelseId() + ")");
            }
            KundhandelseEntity entity = repository.findByKundhandelseId(n.kundhandelseId()).orElseGet(KundhandelseEntity::new);
            repository.save(mapper.toEntity(n, entity));
            antal++;
        }
        return antal;
    }

    /** Hämtar en kundhändelse inklusive taggar (som laddas lazy, därför inom transaktionen). */
    @Transactional(readOnly = true)
    public CachadKundhandelse hamta(String kundhandelseId) {
        KundhandelseEntity e = finn(kundhandelseId);
        return new CachadKundhandelse(mapper.partOf(e), mapper.toDto(e), List.copyOf(e.getTaggar()));
    }

    @Transactional
    public void taBort(String kundhandelseId) {
        repository.delete(finn(kundhandelseId));
    }

    @Transactional(readOnly = true)
    public long antal() {
        return repository.count();
    }

    private KundhandelseEntity finn(String kundhandelseId) {
        return repository.findByKundhandelseId(kundhandelseId).orElseThrow(() -> new SaknasException("Kundhändelsen finns inte: " + kundhandelseId));
    }
}
