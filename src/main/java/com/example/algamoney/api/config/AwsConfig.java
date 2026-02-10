package com.example.algamoney.api.config;

import com.example.algamoney.api.config.property.AlgamoneyApiProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
public class AwsConfig {

	@Autowired
	private AlgamoneyApiProperty property;

	@Bean
	public SesClient sesClient() {
		return SesClient.builder()
				.region(Region.US_EAST_1)
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(
								property.getS3().getAccessKeyId(),
								property.getS3().getSecretAccessKey())))
				.endpointOverride(URI.create("http://localhost:4566"))
				.build();
	}

	@Bean
	public EventBridgeClient eventBridgeClient() {
		return EventBridgeClient.builder()
				.region(Region.US_EAST_1)
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(
								property.getS3().getAccessKeyId(),
								property.getS3().getSecretAccessKey())))
				.endpointOverride(URI.create("http://localhost:4566"))
				.build();
	}

	@Bean
	public SqsAsyncClient sqsAsyncClient() {
		return SqsAsyncClient.builder()
				.region(Region.US_EAST_1)
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(
								property.getS3().getAccessKeyId(),
								property.getS3().getSecretAccessKey())))
				.endpointOverride(URI.create("http://localhost:4566"))
				.build();
	}
}
