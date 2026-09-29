package se.sundsvall.minaarenden.integration.db;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.minaarenden.integration.db.model.KundhandelseEntity;

@Transactional(readOnly = true)
public interface KundhandelseRepository extends JpaRepository<KundhandelseEntity, Long>, JpaSpecificationExecutor<KundhandelseEntity> {

	Optional<KundhandelseEntity> findByMunicipalityIdAndKundhandelseId(String municipalityId, String kundhandelseId);

	long countByMunicipalityId(String municipalityId);
}
