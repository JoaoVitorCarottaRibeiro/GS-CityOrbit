package br.com.fiap.cityorbit.repository;

import br.com.fiap.cityorbit.model.IncidentNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentNotificationRepository extends JpaRepository<IncidentNotificationEntity, Long> {

    List<IncidentNotificationEntity> findTop50ByOrderBySentAtDesc();

    List<IncidentNotificationEntity> findByDeliveredFalseOrderBySentAtDesc();
}
