package br.com.fiap.cityorbit.soap.dto;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

@Data
@XmlRootElement(name = "processSimulationRequest", namespace = "http://cityorbit.fiap.com.br/soap")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProcessSimulationRequest {

    @XmlElement(required = true)
    private Long   cityId;

    @XmlElement(required = true)
    private String simulationType;

    private String  parameters;
    private boolean fetchNasaData;
}
