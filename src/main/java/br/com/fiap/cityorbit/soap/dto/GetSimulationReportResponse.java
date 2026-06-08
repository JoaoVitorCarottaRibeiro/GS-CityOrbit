package br.com.fiap.cityorbit.soap.dto;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

@Data
@XmlRootElement(name = "getSimulationReportResponse", namespace = "http://cityorbit.fiap.com.br/soap")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetSimulationReportResponse {

    @XmlElement(required = true)
    private SimulationReport report;
}
