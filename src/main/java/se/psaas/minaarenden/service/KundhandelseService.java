package se.psaas.minaarenden.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.psaas.minaarenden.api.dto.NyKundhandelse;
import se.psaas.minaarenden.domain.KundhandelseEntity;
import se.psaas.minaarenden.domain.KundhandelseRepository;

/** Underhåll av ärendecachen: verksamhetssystemet skickar in kundhändelser, cachen svarar på frågor. */
@Service
public class KundhandelseService {

    private final KundhandelseRepository repository;
    private final KundhandelseMapper mapper;

    public KundhandelseService(KundhandelseRepository repository, KundhandelseMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** Skapar eller ersätter (samma kundhandelseId) kundhändelser. Returnerar antal sparade. */
    @Transactional
    public int spara(List<NyKundhandelse> nya) {
        int antal = 0;
        for (NyKundhandelse n : nya) {
            if (n.part().isEmpty()) {
                throw new OgiltigFragaException("part måste innehålla kund, ärende eller båda (" + n.kundhandelseId() + ")");
            }
            KundhandelseEntity entity = repository.findByKundhandelseId(n.kundhandelseId()).orElseGet(KundhandelseEntity::new);
            repository.save(mapper.toEntity(n, entity));
            antal++;
        }
        return antal;
    }

    @Transactional(readOnly = true)
    public KundhandelseEntity hamta(String kundhandelseId) {
        return repository.findByKundhandelseId(kundhandelseId).orElseThrow(() -> new SaknasException("Kundhändelsen finns inte: " + kundhandelseId));
    }

    @Transactional
    public void taBort(String kundhandelseId) {
        repository.delete(hamta(kundhandelseId));
    }

    @Transactional(readOnly = true)
    public long antal() {
        return repository.count();
    }
}
