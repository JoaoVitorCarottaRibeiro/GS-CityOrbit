package br.com.fiap.cityorbit.repository;

import br.com.fiap.cityorbit.model.MfaSecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MfaSecretRepository extends JpaRepository<MfaSecretEntity, String> {

    Optional<MfaSecretEntity> findByUsernameAndEnabledTrue(String username);

    boolean existsByUsernameAndEnabledTrue(String username);
}
