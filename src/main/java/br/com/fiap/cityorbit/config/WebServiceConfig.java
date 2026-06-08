package br.com.fiap.cityorbit.config;

import br.com.fiap.cityorbit.soap.dto.*;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "simulation")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema simulationSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setPortTypeName("SimulationPort");
        definition.setLocationUri("/ws");
        definition.setTargetNamespace("http://cityorbit.fiap.com.br/soap");
        definition.setSchema(simulationSchema);
        return definition;
    }

    @Bean
    public XsdSchema simulationSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/simulation.xsd"));
    }

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(
                GetSimulationReportRequest.class,
                GetSimulationReportResponse.class,
                ProcessSimulationRequest.class,
                ProcessSimulationResponse.class,
                SimulationReport.class
        );
        return marshaller;
    }
}
