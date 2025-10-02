package com.example.algamoney.api.storage;

import com.example.algamoney.api.config.property.AlgamoneyApiProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class S3 {

    private static final Logger logger = LoggerFactory.getLogger(S3.class);

    @Autowired
    private AlgamoneyApiProperty property;

    @Autowired
    private S3Client s3Client;

    public String salvarTemporariamente(MultipartFile arquivo) {
        String nomeUnico = gerarNomeUnico(arquivo.getOriginalFilename());

        try {
            Map<String, String> tags = new HashMap<>();
            tags.put("expirar", "true");

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(property.getS3().getBucket())
                    .key(nomeUnico)
                    .contentType(arquivo.getContentType())
                    .contentLength(arquivo.getSize())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .tagging(Tagging.builder()
                            .tagSet(Tag.builder().key("expirar").value("true").build())
                            .build())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(arquivo.getInputStream(), arquivo.getSize()));

            if (logger.isDebugEnabled()) {
                logger.debug("Arquivo {} enviado com sucesso para o S3.", arquivo.getOriginalFilename());
            }

            return nomeUnico;
        } catch (IOException e) {
            throw new RuntimeException("Problemas ao tentar enviar o arquivo para o S3.", e);
        }
    }

    public String configurarUrl(String objeto) {
        return "https://" + property.getS3().getBucket() + ".s3.amazonaws.com/" + objeto;
    }

    public void salvar(String objeto) {
        PutObjectTaggingRequest putTaggingRequest = PutObjectTaggingRequest.builder()
                .bucket(property.getS3().getBucket())
                .key(objeto)
                .tagging(Tagging.builder().build())
                .build();

        s3Client.putObjectTagging(putTaggingRequest);
    }

    public void remover(String objeto) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(property.getS3().getBucket())
                .key(objeto)
                .build();

        s3Client.deleteObject(deleteRequest);
    }

    public void substituir(String objetoAntigo, String objetoNovo) {
        if (StringUtils.hasText(objetoAntigo)) {
            this.remover(objetoAntigo);
        }
        salvar(objetoNovo);
    }

    private String gerarNomeUnico(String originalFilename) {
        return UUID.randomUUID() + "_" + originalFilename;
    }
}