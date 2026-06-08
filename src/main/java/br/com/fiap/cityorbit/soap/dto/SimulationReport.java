package br.com.fiap.cityorbit.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SimulationReport", namespace = "http://cityorbit.fiap.com.br/soap",
         propOrder = {"id","cityName","cityState","simulationType","simulationTypeDescription",
                      "status","riskScore","results","nasaDataReference","createdAt"})
public class SimulationReport {
    private Long   id;
    private String cityName;
    private String cityState;
    private String simulationType;
    private String simulationTypeDescription;
    private String status;
    private Double riskScore;
    private String results;
    private String nasaDataReference;
    private String createdAt;
}
