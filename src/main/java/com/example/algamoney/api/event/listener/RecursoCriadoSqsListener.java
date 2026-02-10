package com.example.algamoney.api.event.listener;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RecursoCriadoSqsListener {

    private static final Logger log = LoggerFactory.getLogger(RecursoCriadoSqsListener.class);

    @SqsListener("recurso-criado-queue")
    public void onRecursoCriado(String message) {
        log.info("Mensagem recebida da fila 'recurso-criado-queue': {}", message);
        // Aqui você pode adicionar a lógica para processar o evento
    }
}
