package br.com.fiap.cityorbit.repository;

import br.com.fiap.cityorbit.model.Simulation;
import br.com.fiap.cityorbit.model.SimulationStatus;
import br.com.fiap.cityorbit.model.SimulationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    List<Simulation> findByCityId(Long cityId);

    List<Simulation> findByStatus(SimulationStatus status);

    List<Simulation> findBySimulationType(SimulationType simulationType);

    List<Simulation> findByCityIdAndSimulationType(Long cityId, SimulationType simulationType);

    long countByCityId(Long cityId);
}
