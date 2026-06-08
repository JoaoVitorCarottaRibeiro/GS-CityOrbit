package br.com.fiap.cityorbit.soap.endpoint;

import br.com.fiap.cityorbit.dto.SimulationRequestDTO;
import br.com.fiap.cityorbit.dto.SimulationResponseDTO;
import br.com.fiap.cityorbit.model.SimulationType;
import br.com.fiap.cityorbit.service.SimulationService;
import br.com.fiap.cityorbit.soap.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
@Slf4j
@RequiredArgsConstructor
public class SimulationEndpoint {

    private static final String NS = "http://cityorbit.fiap.com.br/soap";

    private final SimulationService simulationService;

    @PayloadRoot(namespace = NS, localPart = "getSimulationReportRequest")
    @ResponsePayload
    public GetSimulationReportResponse getSimulationReport(
            @RequestPayload GetSimulationReportRequest request) {

        log.info("SOAP getSimulationReport — simulationId={}", request.getSimulationId());

        SimulationResponseDTO dto = simulationService.findById(request.getSimulationId());

        SimulationReport report = new SimulationReport();
        report.setId(dto.getId());
        report.setCityName(dto.getCityName());
        report.setCityState(dto.getCityState());
        report.setSimulationType(dto.getSimulationType() != null ? dto.getSimulationType().name() : "");
        report.setSimulationTypeDescription(dto.getSimulationTypeDescription());
        report.setStatus(dto.getStatus() != null ? dto.getStatus().name() : "");
        report.setRiskScore(dto.getRiskScore());
        report.setResults(dto.getResults());
        report.setNasaDataReference(dto.getNasaDataReference());
        report.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : "");

        GetSimulationReportResponse response = new GetSimulationReportResponse();
        response.setReport(report);
        return response;
    }

    @PayloadRoot(namespace = NS, localPart = "processSimulationRequest")
    @ResponsePayload
    public ProcessSimulationResponse processSimulation(
            @RequestPayload ProcessSimulationRequest request) {

        log.info("SOAP processSimulation — cityId={}, type={}", request.getCityId(), request.getSimulationType());

        SimulationRequestDTO dto = new SimulationRequestDTO();
        dto.setCityId(request.getCityId());
        dto.setSimulationType(SimulationType.valueOf(request.getSimulationType().toUpperCase()));
        dto.setParameters(request.getParameters());
        dto.setFetchNasaData(request.isFetchNasaData());

        SimulationResponseDTO created   = simulationService.create(dto);
        SimulationResponseDTO processed = simulationService.process(created.getId());

        ProcessSimulationResponse response = new ProcessSimulationResponse();
        response.setSimulationId(processed.getId());
        response.setStatus(processed.getStatus().name());
        response.setRiskScore(processed.getRiskScore());
        response.setMessage(buildMessage(processed));
        return response;
    }

    private String buildMessage(SimulationResponseDTO dto) {
        if (dto.getRiskScore() == null) return "Simulação processada";
        double risk = dto.getRiskScore();
        if (risk > 0.7) return "ALTO RISCO — Ação imediata necessária";
        if (risk > 0.4) return "MÉDIO RISCO — Monitoramento recomendado";
        return "BAIXO RISCO — Situação estável";
    }
}
