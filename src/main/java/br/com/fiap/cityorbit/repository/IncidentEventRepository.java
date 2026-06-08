package br.com.fiap.cityorbit.repository;

import br.com.fiap.cityorbit.model.IncidentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentEventRepository extends JpaRepository<IncidentEventEntity, Long> {

    List<IncidentEventEntity> findTop20ByOrderByTimestampDesc();

    Optional<IncidentEventEntity> findFirstByOrderByTimestampDesc();
}
