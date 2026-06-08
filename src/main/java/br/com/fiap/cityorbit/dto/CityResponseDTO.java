package br.com.fiap.cityorbit.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CityResponseDTO {

    private Long id;
    private String name;
    private String state;
    private String country;
    private Double latitude;
    private Double longitude;
    private Long population;
    private Double areaKm2;
    private String satelliteResolution;
    private Boolean lidarAvailable;
    private int totalSimulations;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
