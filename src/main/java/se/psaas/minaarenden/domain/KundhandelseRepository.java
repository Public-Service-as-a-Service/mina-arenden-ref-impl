package se.psaas.minaarenden.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface KundhandelseRepository extends JpaRepository<KundhandelseEntity, Long>, JpaSpecificationExecutor<KundhandelseEntity> {

    Optional<KundhandelseEntity> findByKundhandelseId(String kundhandelseId);

    boolean existsByKundhandelseId(String kundhandelseId);
}
