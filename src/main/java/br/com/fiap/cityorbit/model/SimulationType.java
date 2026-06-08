package br.com.fiap.cityorbit.model;

public enum SimulationType {

    FLOOD("Simulação de Enchente", 0.7),
    TRAFFIC("Simulação de Tráfego", 0.5),
    CONSTRUCTION("Simulação de Impacto de Obra", 0.4),
    ZONING("Simulação de Mudança de Zoneamento", 0.3),
    HEAT_ISLAND("Simulação de Ilha de Calor", 0.6);

    private final String description;
    private final double baseRiskWeight;

    SimulationType(String description, double baseRiskWeight) {
        this.description = description;
        this.baseRiskWeight = baseRiskWeight;
    }

    public String getDescription() {
        return description;
    }

    public double getBaseRiskWeight() {
        return baseRiskWeight;
    }
}
