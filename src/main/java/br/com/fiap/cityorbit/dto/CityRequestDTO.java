package br.com.fiap.cityorbit.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CityRequestDTO {

    @NotBlank(message = "Nome da cidade é obrigatório")
    private String name;

    @NotBlank(message = "Estado é obrigatório")
    private String state;

    @NotBlank(message = "País é obrigatório")
    private String country;

    @NotNull(message = "Latitude é obrigatória")
    @DecimalMin(value = "-90.0",  message = "Latitude mínima: -90")
    @DecimalMax(value = "90.0",   message = "Latitude máxima: 90")
    private Double latitude;

    @NotNull(message = "Longitude é obrigatória")
    @DecimalMin(value = "-180.0", message = "Longitude mínima: -180")
    @DecimalMax(value = "180.0",  message = "Longitude máxima: 180")
    private Double longitude;

    @Min(value = 0, message = "População não pode ser negativa")
    private Long population;

    @DecimalMin(value = "0.0", message = "Área não pode ser negativa")
    private Double areaKm2;

    private String satelliteResolution;
    private Boolean lidarAvailable;
}
