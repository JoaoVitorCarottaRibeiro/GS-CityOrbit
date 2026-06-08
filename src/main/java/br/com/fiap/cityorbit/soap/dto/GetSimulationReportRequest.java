package br.com.fiap.cityorbit.soap.dto;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

@Data
@XmlRootElement(name = "getSimulationReportRequest", namespace = "http://cityorbit.fiap.com.br/soap")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetSimulationReportRequest {

    @XmlElement(required = true)
    private Long simulationId;
}
