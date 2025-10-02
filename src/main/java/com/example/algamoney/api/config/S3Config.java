package com.example.algamoney.api.config;

import com.example.algamoney.api.config.property.AlgamoneyApiProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Configuration
public class S3Config {

    @Autowired
    private AlgamoneyApiProperty property;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                property.getS3().getAccessKeyId(),
                property.getS3().getSecretAccessKey());

        S3Client s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.US_EAST_1)
                .build();

        try {
            // Verifica se o bucket existe
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(property.getS3().getBucket())
                    .build());
        } catch (NoSuchBucketException e) {
            // Cria o bucket se não existir
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(property.getS3().getBucket())
                    .build());

            LifecycleRule regraExpiracao = LifecycleRule.builder()
                    .id("Regra de expiração de arquivos temporários")
                    .filter(LifecycleRuleFilter.builder()
                            .tag(Tag.builder()
                                    .key("expirar")
                                    .value("true")
                                    .build())
                            .build())
                    .expiration(LifecycleExpiration.builder()
                            .days(1)
                            .build())
                    .status(ExpirationStatus.ENABLED)
                    .build();

            BucketLifecycleConfiguration configuration = BucketLifecycleConfiguration.builder()
                    .rules(regraExpiracao)
                    .build();

            s3Client.putBucketLifecycleConfiguration(PutBucketLifecycleConfigurationRequest.builder()
                    .bucket(property.getS3().getBucket())
                    .lifecycleConfiguration(configuration)
                    .build());
        }

        return s3Client;
    }
}