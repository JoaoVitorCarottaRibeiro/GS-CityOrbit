package br.com.fiap.cityorbit.service;

import br.com.fiap.cityorbit.dto.CityRequestDTO;
import br.com.fiap.cityorbit.dto.CityResponseDTO;
import br.com.fiap.cityorbit.exception.BusinessException;
import br.com.fiap.cityorbit.exception.ResourceNotFoundException;
import br.com.fiap.cityorbit.model.City;
import br.com.fiap.cityorbit.repository.CityRepository;
import br.com.fiap.cityorbit.repository.SimulationRepository;
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
class CityServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private SimulationRepository simulationRepository;

    @InjectMocks
    private CityService cityService;

    // ── findAll ─────────────────────────────────────────────────────────────

    @Test
    void findAll_deveRetornarListaDeDTOs() {
        City city = buildCity(1L, "São Paulo", "SP");
        when(cityRepository.findAll()).thenReturn(List.of(city));
        when(simulationRepository.countByCityId(1L)).thenReturn(3L);

        List<CityResponseDTO> result = cityService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("São Paulo");
        assertThat(result.get(0).getTotalSimulations()).isEqualTo(3);
    }

    // ── findById ────────────────────────────────────────────────────────────

    @Test
    void findById_deveRetornarDTOQuandoEncontrado() {
        City city = buildCity(1L, "São Paulo", "SP");
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(simulationRepository.countByCityId(1L)).thenReturn(0L);

        CityResponseDTO dto = cityService.findById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("São Paulo");
    }

    @Test
    void findById_deveLancarExcecaoQuandoNaoEncontrado() {
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void create_deveSalvarCidade() {
        CityRequestDTO dto = buildRequest("Rio de Janeiro", "RJ");
        City saved = buildCity(1L, "Rio de Janeiro", "RJ");

        when(cityRepository.existsByNameIgnoreCaseAndStateIgnoreCase("Rio de Janeiro", "RJ"))
                .thenReturn(false);
        when(cityRepository.save(any())).thenReturn(saved);
        when(simulationRepository.countByCityId(1L)).thenReturn(0L);

        CityResponseDTO result = cityService.create(dto);

        assertThat(result.getName()).isEqualTo("Rio de Janeiro");
        verify(cityRepository).save(any());
    }

    @Test
    void create_deveLancarExcecaoQuandoCidadeDuplicada() {
        CityRequestDTO dto = buildRequest("São Paulo", "SP");

        when(cityRepository.existsByNameIgnoreCaseAndStateIgnoreCase("São Paulo", "SP"))
                .thenReturn(true);

        assertThatThrownBy(() -> cityService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já está cadastrada");
    }

    // ── update ──────────────────────────────────────────────────────────────

    @Test
    void update_deveAtualizarCidade() {
        City existing = buildCity(1L, "São Paulo", "SP");
        CityRequestDTO dto = buildRequest("São Paulo Atualizado", "SP");

        when(cityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(simulationRepository.countByCityId(1L)).thenReturn(0L);

        CityResponseDTO result = cityService.update(1L, dto);

        assertThat(result.getName()).isEqualTo("São Paulo Atualizado");
    }

    // ── delete ──────────────────────────────────────────────────────────────

    @Test
    void delete_deveDeletarCidade() {
        when(cityRepository.existsById(1L)).thenReturn(true);

        cityService.delete(1L);

        verify(cityRepository).deleteById(1L);
    }

    @Test
    void delete_deveLancarExcecaoQuandoNaoEncontrada() {
        when(cityRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> cityService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getSummary (Polimorfismo) ────────────────────────────────────────────

    @Test
    void getSummary_deveRetornarDescricaoEspecificaDeCidade() {
        City city = buildCity(1L, "Manaus", "AM");
        city.setPopulation(2219580L);
        city.setAreaKm2(11401.09);
        city.setLidarAvailable(false);

        String summary = city.getSummary();

        assertThat(summary).contains("Manaus", "AM", "2219580");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private City buildCity(Long id, String name, String state) {
        City c = new City();
        c.setId(id);
        c.setName(name);
        c.setState(state);
        c.setCountry("Brasil");
        c.setLatitude(-23.0);
        c.setLongitude(-46.0);
        return c;
    }

    private CityRequestDTO buildRequest(String name, String state) {
        CityRequestDTO dto = new CityRequestDTO();
        dto.setName(name);
        dto.setState(state);
        dto.setCountry("Brasil");
        dto.setLatitude(-23.0);
        dto.setLongitude(-46.0);
        return dto;
    }
}
