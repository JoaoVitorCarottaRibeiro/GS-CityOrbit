package br.com.fiap.cityorbit.service;

import br.com.fiap.cityorbit.dto.NasaClimateDataDTO;
import br.com.fiap.cityorbit.model.SimulationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NasaIntegrationService {

    private final RestTemplate restTemplate;

    private static final String NASA_POWER_BASE_URL = "https://power.larc.nasa.gov/api/temporal/daily/point";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public NasaClimateDataDTO fetchClimateData(Double latitude, Double longitude) {
        LocalDate end   = LocalDate.now().minusDays(2);
        LocalDate start = end.minusDays(29);

        String url = UriComponentsBuilder.fromHttpUrl(NASA_POWER_BASE_URL)
                .queryParam("parameters",  "T2M,PRECTOTCORR,WS10M,RH2M")
                .queryParam("community",   "RE")
                .queryParam("longitude",   String.format("%.4f", longitude))
                .queryParam("latitude",    String.format("%.4f", latitude))
                .queryParam("start",       start.format(DATE_FMT))
                .queryParam("end",         end.format(DATE_FMT))
                .queryParam("format",      "JSON")
                .toUriString();

        log.info("Consultando NASA POWER API — lat={}, lon={}", latitude, longitude);
        log.debug("URL: {}", url);

        try {
            NasaClimateDataDTO data = restTemplate.getForObject(url, NasaClimateDataDTO.class);
            log.info("Dados climáticos recebidos da NASA com sucesso");
            return data;
        } catch (Exception e) {
            log.warn("NASA POWER API indisponível ou erro de rede: {}", e.getMessage());
            return null;
        }
    }

    public String buildReference(Double latitude, Double longitude) {
        LocalDate end   = LocalDate.now().minusDays(2);
        LocalDate start = end.minusDays(29);
        return String.format("NASA_POWER|lat=%.4f|lon=%.4f|start=%s|end=%s",
                latitude, longitude, start.format(DATE_FMT), end.format(DATE_FMT));
    }

    public double calculateRiskScore(NasaClimateDataDTO data, SimulationType type) {
        if (data == null || data.getProperties() == null) {
            return type.getBaseRiskWeight();
        }
        try {
            Map<String, Map<String, Double>> params = data.getProperties().getParameter();
            return switch (type) {
                case FLOOD       -> calcFloodRisk(params);
                case HEAT_ISLAND -> calcHeatIslandRisk(params);
                case TRAFFIC     -> calcTrafficRisk(params);
                default          -> type.getBaseRiskWeight();
            };
        } catch (Exception e) {
            log.warn("Erro ao calcular risco para {}: {}", type, e.getMessage());
            return type.getBaseRiskWeight();
        }
    }

    private double calcFloodRisk(Map<String, Map<String, Double>> params) {
        Map<String, Double> precip = params.get("PRECTOTCORR");
        if (precip == null) return 0.5;
        double totalMm = precip.values().stream().mapToDouble(v -> v > 0 ? v : 0).sum();
        return Math.min(1.0, Math.max(0.0, totalMm / 120.0));
    }

    private double calcHeatIslandRisk(Map<String, Map<String, Double>> params) {
        Map<String, Double> temp = params.get("T2M");
        if (temp == null) return 0.5;
        double avgTemp = temp.values().stream().mapToDouble(Double::doubleValue).average().orElse(25.0);
        return Math.min(1.0, Math.max(0.0, (avgTemp - 20.0) / 15.0));
    }

    private double calcTrafficRisk(Map<String, Map<String, Double>> params) {
        Map<String, Double> wind = params.get("WS10M");
        Map<String, Double> rain = params.get("PRECTOTCORR");
        double windFactor = 0.0;
        double rainFactor = 0.0;
        if (wind != null) {
            double avgWind = wind.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            windFactor = Math.min(1.0, avgWind / 15.0);
        }
        if (rain != null) {
            double totalRain = rain.values().stream().mapToDouble(v -> v > 0 ? v : 0).sum();
            rainFactor = Math.min(1.0, totalRain / 80.0);
        }
        return Math.min(1.0, 0.4 + (windFactor * 0.3) + (rainFactor * 0.3));
    }
}
