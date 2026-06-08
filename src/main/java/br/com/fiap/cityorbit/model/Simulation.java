package br.com.fiap.cityorbit.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "simulations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Simulation extends BaseEntity {

    @NotNull(message = "Tipo de simulação é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "simulation_type", nullable = false)
    private SimulationType simulationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SimulationStatus status = SimulationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String parameters;

    @Column(columnDefinition = "TEXT")
    private String results;

    @Column(name = "nasa_data_reference")
    private String nasaDataReference;

    @Column(name = "climate_data", columnDefinition = "TEXT")
    private String climateData;

    @Column(name = "risk_score")
    private Double riskScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Override
    public String getSummary() {
        return String.format("Simulação #%s [%s] — Cidade: %s — Status: %s — Risco: %.0f%%",
                getId() != null ? getId() : "N/A",
                simulationType != null ? simulationType.getDescription() : "N/A",
                city != null ? city.getName() : "N/A",
                status,
                riskScore != null ? riskScore * 100 : 0.0);
    }
}
