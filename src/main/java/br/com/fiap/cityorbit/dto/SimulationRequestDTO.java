package br.com.fiap.cityorbit.dto;

import br.com.fiap.cityorbit.model.SimulationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SimulationRequestDTO {

    @NotNull(message = "ID da cidade é obrigatório")
    private Long cityId;

    @NotNull(message = "Tipo de simulação é obrigatório")
    private SimulationType simulationType;

    private String parameters;

    private Boolean fetchNasaData = false;
}
