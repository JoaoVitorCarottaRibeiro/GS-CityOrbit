package br.com.fiap.cityorbit.soap.dto;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

@Data
@XmlRootElement(name = "processSimulationResponse", namespace = "http://cityorbit.fiap.com.br/soap")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProcessSimulationResponse {

    private Long   simulationId;
    private String status;
    private String message;
    private Double riskScore;
}
