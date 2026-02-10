package com.example.algamoney.api.event.producer;

import com.example.algamoney.api.event.RecursoCriadoEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.net.URI;
import java.util.Map;

@Component
public class RecursoCriadoProducer implements ApplicationListener<RecursoCriadoEvent> {

    private static final Logger log = LoggerFactory.getLogger(RecursoCriadoProducer.class);

    @Autowired
    private EventBridgeClient eventBridgeClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onApplicationEvent(RecursoCriadoEvent recursoCriadoEvent) {
        HttpServletResponse response = recursoCriadoEvent.getResponse();
        Long codigo = recursoCriadoEvent.getCodigo();

        URI uri = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{codigo}")
                .buildAndExpand(codigo).toUri();

        adicionarHeaderLocation(response, uri);
        publicarEventoNoEventBridge(codigo, uri);
    }

    private void adicionarHeaderLocation(HttpServletResponse response, URI uri) {
        response.setHeader("Location", uri.toASCIIString());
        log.info("Header 'Location' adicionado com a URI: {}", uri.toASCIIString());
    }

    private void publicarEventoNoEventBridge(Long codigo, URI uri) {
        try {
            Map<String, Object> eventDetail = Map.of(
                    "codigo", codigo,
                    "uri", uri.toASCIIString()
            );

            PutEventsRequestEntry requestEntry = PutEventsRequestEntry.builder()
                    .source("algamoney-api")
                    .detailType("RecursoCriado")
                    .detail(objectMapper.writeValueAsString(eventDetail))
                    .eventBusName("algamoney-events")
                    .build();

            PutEventsRequest request = PutEventsRequest.builder()
                    .entries(requestEntry)
                    .build();

            eventBridgeClient.putEvents(request);
            log.info("Evento 'RecursoCriado' publicado no EventBridge para o código: {}", codigo);
        } catch (Exception e) {
            log.error("Erro ao publicar evento 'RecursoCriado' no EventBridge para o código: {}", codigo, e);
        }
    }
}
