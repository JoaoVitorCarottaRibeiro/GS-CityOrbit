package br.com.fiap.cityorbit.dto;

import br.com.fiap.cityorbit.model.SimulationStatus;
import br.com.fiap.cityorbit.model.SimulationType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SimulationResponseDTO {

    private Long id;
    private Long cityId;
    private String cityName;
    private String cityState;
    private SimulationType simulationType;
    private String simulationTypeDescription;
    private SimulationStatus status;
    private String parameters;
    private String results;
    private String nasaDataReference;
    private String climateData;
    private Double riskScore;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
