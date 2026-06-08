package br.com.fiap.cityorbit.repository;

import br.com.fiap.cityorbit.model.ConsentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecordEntity, Long> {

    Optional<ConsentRecordEntity> findFirstByUsernameOrderByTimestampDesc(String username);
}
