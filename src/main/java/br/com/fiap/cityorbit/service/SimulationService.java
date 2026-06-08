package br.com.fiap.cityorbit.service;

import br.com.fiap.cityorbit.dto.NasaClimateDataDTO;
import br.com.fiap.cityorbit.dto.SimulationRequestDTO;
import br.com.fiap.cityorbit.dto.SimulationResponseDTO;
import br.com.fiap.cityorbit.exception.ResourceNotFoundException;
import br.com.fiap.cityorbit.model.City;
import br.com.fiap.cityorbit.model.Simulation;
import br.com.fiap.cityorbit.model.SimulationStatus;
import br.com.fiap.cityorbit.model.SimulationType;
import br.com.fiap.cityorbit.repository.SimulationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final CityService cityService;
    private final NasaIntegrationService nasaService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SimulationResponseDTO> findAll() {
        return simulationRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SimulationResponseDTO findById(Long id) {
        return toResponseDTO(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<SimulationResponseDTO> findByCityId(Long cityId) {
        return simulationRepository.findByCityId(cityId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SimulationResponseDTO> findByStatus(SimulationStatus status) {
        return simulationRepository.findByStatus(status).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SimulationResponseDTO> findByType(SimulationType type) {
        return simulationRepository.findBySimulationType(type).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SimulationResponseDTO create(SimulationRequestDTO dto) {
        City city = cityService.getOrThrow(dto.getCityId());

        Simulation sim = new Simulation();
        sim.setCity(city);
        sim.setSimulationType(dto.getSimulationType());
        sim.setParameters(dto.getParameters());
        sim.setStatus(SimulationStatus.PENDING);

        if (Boolean.TRUE.equals(dto.getFetchNasaData())) {
            enrichWithNasaData(sim, city);
        }

        Simulation saved = simulationRepository.save(sim);
        log.info("Simulação criada: ID={} tipo={} cidade={}", saved.getId(), saved.getSimulationType(), city.getName());
        return toResponseDTO(saved);
    }

    @Transactional
    public SimulationResponseDTO update(Long id, SimulationRequestDTO dto) {
        Simulation sim = getOrThrow(id);
        City city = cityService.getOrThrow(dto.getCityId());
        sim.setCity(city);
        sim.setSimulationType(dto.getSimulationType());
        sim.setParameters(dto.getParameters());
        return toResponseDTO(simulationRepository.save(sim));
    }

    @Transactional
    public void delete(Long id) {
        if (!simulationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Simulação", id);
        }
        simulationRepository.deleteById(id);
        log.info("Simulação deletada — ID {}", id);
    }

    @Transactional
    public SimulationResponseDTO process(Long id) {
        Simulation sim = getOrThrow(id);
        sim.setStatus(SimulationStatus.PROCESSING);
        simulationRepository.save(sim);

        try {
            enrichWithNasaData(sim, sim.getCity());
            sim.setResults(buildResults(sim));
            sim.setStatus(SimulationStatus.COMPLETED);
            log.info("Simulação {} processada — risco={}", id, sim.getRiskScore());
        } catch (Exception e) {
            log.error("Erro ao processar simulação {}: {}", id, e.getMessage());
            sim.setStatus(SimulationStatus.FAILED);
        }

        return toResponseDTO(simulationRepository.save(sim));
    }

    private void enrichWithNasaData(Simulation sim, City city) {
        try {
            NasaClimateDataDTO nasaData = nasaService.fetchClimateData(
                    city.getLatitude(), city.getLongitude());

            if (nasaData != null) {
                String json = objectMapper.writeValueAsString(nasaData);
                sim.setClimateData(json.length() > 3000 ? json.substring(0, 3000) + "..." : json);
                sim.setNasaDataReference(nasaService.buildReference(city.getLatitude(), city.getLongitude()));
                sim.setRiskScore(nasaService.calculateRiskScore(nasaData, sim.getSimulationType()));
            } else {
                sim.setRiskScore(sim.getSimulationType().getBaseRiskWeight());
            }
        } catch (Exception e) {
            log.warn("Não foi possível enriquecer com dados NASA: {}", e.getMessage());
            sim.setRiskScore(sim.getSimulationType().getBaseRiskWeight());
        }
    }

    private String buildResults(Simulation sim) {
        double risk = sim.getRiskScore() != null ? sim.getRiskScore() : 0.0;
        String level   = risk > 0.7 ? "ALTO"   : risk > 0.4 ? "MÉDIO"  : "BAIXO";
        String action  = risk > 0.7 ? "Ação imediata necessária"
                       : risk > 0.4 ? "Monitoramento recomendado"
                       : "Situação estável — monitoramento de rotina";

        return String.format(
            "{\"type\":\"%s\",\"riskScore\":%.2f,\"riskLevel\":\"%s\",\"summary\":\"%s\",\"recommendation\":\"%s\"}",
            sim.getSimulationType(),
            risk, level,
            sim.getSimulationType().getDescription() + " processada com dados de satélite NASA",
            action);
    }

    public SimulationResponseDTO toResponseDTO(Simulation s) {
        return SimulationResponseDTO.builder()
                .id(s.getId())
                .cityId(s.getCity() != null ? s.getCity().getId() : null)
                .cityName(s.getCity() != null ? s.getCity().getName() : null)
                .cityState(s.getCity() != null ? s.getCity().getState() : null)
                .simulationType(s.getSimulationType())
                .simulationTypeDescription(s.getSimulationType() != null
                        ? s.getSimulationType().getDescription() : null)
                .status(s.getStatus())
                .parameters(s.getParameters())
                .results(s.getResults())
                .nasaDataReference(s.getNasaDataReference())
                .climateData(s.getClimateData())
                .riskScore(s.getRiskScore())
                .summary(s.getSummary())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    public Simulation getOrThrow(Long id) {
        return simulationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Simulação", id));
    }
}
