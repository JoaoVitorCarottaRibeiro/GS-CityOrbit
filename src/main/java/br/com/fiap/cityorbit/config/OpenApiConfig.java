package br.com.fiap.cityorbit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cityOrbitOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CityOrbit API")
                        .description(
                                "API REST do CityOrbit - Plataforma de Gemeo Digital Urbano.\n\n" +
                                "Utiliza imagens de satelite de alta resolucao, LiDAR e IA generativa para simular " +
                                "impactos de obras, enchentes, mudancas de zoneamento e trafego.\n\n" +
                                "Integracao com NASA POWER API para dados climaticos reais.\n" +
                                "Web Service SOAP disponivel em: /ws/simulation.wsdl\n" +
                                "H2 Console (dev only): /h2-console"
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FIAP - Global Solution 2026")
                                .email("contato@cityorbit.com.br"))
                        .license(new License().name("Apache 2.0")));
    }
}
