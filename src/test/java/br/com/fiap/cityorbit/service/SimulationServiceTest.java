package br.com.fiap.cityorbit.service;

import br.com.fiap.cityorbit.dto.SimulationRequestDTO;
import br.com.fiap.cityorbit.dto.SimulationResponseDTO;
import br.com.fiap.cityorbit.exception.ResourceNotFoundException;
import br.com.fiap.cityorbit.model.*;
import br.com.fiap.cityorbit.repository.SimulationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock
    private SimulationRepository simulationRepository;

    @Mock
    private CityService cityService;

    @Mock
    private NasaIntegrationService nasaIntegrationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SimulationService simulationService;

    // ── findAll ─────────────────────────────────────────────────────────────

    @Test
    void findAll_deveRetornarListaDeSimulacoes() {
        Simulation sim = buildSimulation(1L, SimulationType.FLOOD, SimulationStatus.PENDING);
        when(simulationRepository.findAll()).thenReturn(List.of(sim));

        List<SimulationResponseDTO> result = simulationService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSimulationType()).isEqualTo(SimulationType.FLOOD);
    }

    // ── findById ────────────────────────────────────────────────────────────

    @Test
    void findById_deveLancarExcecaoQuandoNaoEncontrada() {
        when(simulationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> simulationService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void create_deveSalvarSimulacao() {
        City city = buildCity(1L);
        Simulation saved = buildSimulation(1L, SimulationType.TRAFFIC, SimulationStatus.PENDING);
        saved.setCity(city);

        SimulationRequestDTO dto = new SimulationRequestDTO();
        dto.setCityId(1L);
        dto.setSimulationType(SimulationType.TRAFFIC);
        dto.setFetchNasaData(false);

        when(cityService.getOrThrow(1L)).thenReturn(city);
        when(simulationRepository.save(any())).thenReturn(saved);

        SimulationResponseDTO result = simulationService.create(dto);

        assertThat(result.getSimulationType()).isEqualTo(SimulationType.TRAFFIC);
        verify(simulationRepository).save(any());
    }

    // ── delete ──────────────────────────────────────────────────────────────

    @Test
    void delete_deveDeletarSimulacao() {
        when(simulationRepository.existsById(1L)).thenReturn(true);

        simulationService.delete(1L);

        verify(simulationRepository).deleteById(1L);
    }

    @Test
    void delete_deveLancarExcecaoQuandoNaoEncontrada() {
        when(simulationRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> simulationService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getSummary (Polimorfismo) ────────────────────────────────────────────

    @Test
    void getSummary_deveRetornarDescricaoEspecificaDeSimulacao() {
        City city = buildCity(1L);
        Simulation sim = buildSimulation(1L, SimulationType.FLOOD, SimulationStatus.COMPLETED);
        sim.setCity(city);
        sim.setRiskScore(0.85);

        String summary = sim.getSummary();

        assertThat(summary).contains("Simulação de Enchente", "São Paulo", "COMPLETED", "85%");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Simulation buildSimulation(Long id, SimulationType type, SimulationStatus status) {
        Simulation s = new Simulation();
        s.setId(id);
        s.setSimulationType(type);
        s.setStatus(status);
        return s;
    }

    private City buildCity(Long id) {
        City c = new City();
        c.setName("São Paulo");
        c.setState("SP");
        c.setCountry("Brasil");
        c.setLatitude(-23.5505);
        c.setLongitude(-46.6333);
        return c;
    }
}
