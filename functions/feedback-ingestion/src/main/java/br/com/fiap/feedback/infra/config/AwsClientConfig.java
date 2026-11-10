package br.com.fiap.feedback.infra.config;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Clientes da AWS, só no build do profile lambda. Região e credenciais vêm do ambiente da Lambda. */
@IfBuildProperty(name = "feedback.aws.enabled", stringValue = "true")
public class AwsClientConfig {

    @Produces
    @Singleton
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
    }

    @Produces
    @Singleton
    public SqsClient sqsClient() {
        return SqsClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
    }
}
