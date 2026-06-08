package br.com.fiap.cityorbit.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class City extends BaseEntity {

    @NotBlank(message = "Nome da cidade é obrigatório")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Estado é obrigatório")
    @Column(nullable = false)
    private String state;

    @NotBlank(message = "País é obrigatório")
    @Column(nullable = false)
    private String country;

    @NotNull(message = "Latitude é obrigatória")
    @DecimalMin(value = "-90.0", message = "Latitude mínima: -90")
    @DecimalMax(value = "90.0",  message = "Latitude máxima: 90")
    @Column(nullable = false)
    private Double latitude;

    @NotNull(message = "Longitude é obrigatória")
    @DecimalMin(value = "-180.0", message = "Longitude mínima: -180")
    @DecimalMax(value = "180.0",  message = "Longitude máxima: 180")
    @Column(nullable = false)
    private Double longitude;

    @Min(value = 0, message = "População não pode ser negativa")
    private Long population;

    @DecimalMin(value = "0.0", message = "Área não pode ser negativa")
    @Column(name = "area_km2")
    private Double areaKm2;

    @Column(name = "satellite_resolution")
    private String satelliteResolution;

    @Column(name = "lidar_available")
    private Boolean lidarAvailable = false;

    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Simulation> simulations = new ArrayList<>();

    @Override
    public String getSummary() {
        return String.format("Cidade: %s/%s — Pop: %d — Área: %.2f km² — LiDAR: %s",
                name, state,
                population != null ? population : 0,
                areaKm2 != null ? areaKm2 : 0.0,
                Boolean.TRUE.equals(lidarAvailable) ? "Disponível" : "Indisponível");
    }
}
