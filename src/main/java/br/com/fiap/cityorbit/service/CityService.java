package br.com.fiap.cityorbit.service;

import br.com.fiap.cityorbit.dto.CityRequestDTO;
import br.com.fiap.cityorbit.dto.CityResponseDTO;
import br.com.fiap.cityorbit.exception.BusinessException;
import br.com.fiap.cityorbit.exception.ResourceNotFoundException;
import br.com.fiap.cityorbit.model.City;
import br.com.fiap.cityorbit.repository.CityRepository;
import br.com.fiap.cityorbit.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final SimulationRepository simulationRepository;

    @Transactional(readOnly = true)
    public List<CityResponseDTO> findAll() {
        return cityRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CityResponseDTO findById(Long id) {
        City city = getOrThrow(id);
        return toResponseDTO(city);
    }

    @Transactional(readOnly = true)
    public List<CityResponseDTO> findByCountry(String country) {
        return cityRepository.findByCountryIgnoreCase(country).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CityResponseDTO> findByState(String state) {
        return cityRepository.findByStateIgnoreCase(state).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CityResponseDTO create(CityRequestDTO dto) {
        if (cityRepository.existsByNameIgnoreCaseAndStateIgnoreCase(dto.getName(), dto.getState())) {
            throw new BusinessException(
                    "Cidade '" + dto.getName() + "/" + dto.getState() + "' já está cadastrada");
        }
        City saved = cityRepository.save(fromDTO(dto));
        log.info("Cidade criada: {} — ID {}", saved.getName(), saved.getId());
        return toResponseDTO(saved);
    }

    @Transactional
    public CityResponseDTO update(Long id, CityRequestDTO dto) {
        City city = getOrThrow(id);
        applyDTO(city, dto);
        City updated = cityRepository.save(city);
        log.info("Cidade atualizada: {} — ID {}", updated.getName(), updated.getId());
        return toResponseDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!cityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cidade", id);
        }
        cityRepository.deleteById(id);
        log.info("Cidade deletada — ID {}", id);
    }

    private City fromDTO(CityRequestDTO dto) {
        City c = new City();
        applyDTO(c, dto);
        return c;
    }

    private void applyDTO(City c, CityRequestDTO dto) {
        c.setName(dto.getName());
        c.setState(dto.getState());
        c.setCountry(dto.getCountry());
        c.setLatitude(dto.getLatitude());
        c.setLongitude(dto.getLongitude());
        c.setPopulation(dto.getPopulation());
        c.setAreaKm2(dto.getAreaKm2());
        c.setSatelliteResolution(dto.getSatelliteResolution());
        c.setLidarAvailable(dto.getLidarAvailable() != null ? dto.getLidarAvailable() : false);
    }

    public CityResponseDTO toResponseDTO(City c) {
        long totalSims = simulationRepository.countByCityId(c.getId() != null ? c.getId() : 0L);
        return CityResponseDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .state(c.getState())
                .country(c.getCountry())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .population(c.getPopulation())
                .areaKm2(c.getAreaKm2())
                .satelliteResolution(c.getSatelliteResolution())
                .lidarAvailable(c.getLidarAvailable())
                .totalSimulations((int) totalSims)
                .summary(c.getSummary())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    public City getOrThrow(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cidade", id));
    }
}
