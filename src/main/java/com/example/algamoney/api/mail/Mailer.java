package com.example.algamoney.api.mail;

import com.example.algamoney.api.model.Lancamento;
import com.example.algamoney.api.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class Mailer {

    private static final Logger log = LoggerFactory.getLogger(Mailer.class);

    @Autowired
    private TemplateEngine thymeleaf;

    @Autowired
    private SesClient sesClient;

    public void avisarSobreLancamentosVencidos(
            List<Lancamento> vencidos, List<Usuario> destinatarios) {
        log.info("Iniciando processo de aviso sobre lançamentos vencidos.");
        Map<String, Object> variaveis = new HashMap<>();
        variaveis.put("lancamentos", vencidos);

        List<String> emails = destinatarios.stream()
                .map(Usuario::getEmail)
                .collect(Collectors.toList());

        log.debug("Enviando e-mail de aviso para {} destinatários.", emails.size());
        this.enviarEmail("testes.algaworks@gmail.com",
                emails,
                "Lançamentos vencidos",
                "mail/aviso-lancamentos-vencidos",
                variaveis);
        log.info("Processo de aviso sobre lançamentos vencidos concluído.");
    }

    public void enviarEmail(String remetente,
                            List<String> destinatarios, String assunto, String template,
                            Map<String, Object> variaveis) {
        log.debug("Processando template de e-mail: {}", template);
        Context context = new Context(new Locale("pt", "BR"));
        variaveis.forEach(context::setVariable);
        String mensagem = thymeleaf.process(template, context);
        log.debug("Template processado com sucesso.");

        this.enviarEmail(remetente, destinatarios, assunto, mensagem);
    }

    public void enviarEmail(String remetente,
                            List<String> destinatarios, String assunto, String mensagem) {
        log.info("Enviando e-mail. Assunto: '{}', Remetente: '{}', Destinatários: {}", assunto, remetente, destinatarios);
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(remetente)
                    .destination(Destination.builder().toAddresses(destinatarios).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(assunto).build())
                            .body(Body.builder()
                                    .html(Content.builder().data(mensagem).build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(request);
            log.info("E-mail enviado com sucesso para: {}", destinatarios);
        } catch (SesException e) {
            log.error("Erro ao enviar e-mail via SES. Assunto: '{}', Destinatários: {}", assunto, destinatarios, e);
            throw new RuntimeException("Problemas com o envio de e-mail!", e);
        }
    }
}
