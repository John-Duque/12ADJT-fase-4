package br.com.fiap.feedback.infra.persistence;

import br.com.fiap.feedback.application.feedback.gateway.FeedbackGateway;
import br.com.fiap.feedback.domain.feedback.Feedback;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/** Grava o feedback na tabela do DynamoDB. Substitui o InMemoryFeedbackGateway no build do profile lambda. */
@ApplicationScoped
@IfBuildProperty(name = "feedback.aws.enabled", stringValue = "true")
public class DynamoDbFeedbackGateway implements FeedbackGateway {

    /** Data com tamanho fixo, para a ordenação de texto do índice por-dia seguir a ordem do tempo. */
    public static final DateTimeFormatter CREATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'").withZone(ZoneOffset.UTC);
    public static final DateTimeFormatter CREATED_DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    private final DynamoDbClient client;
    private final String tableName;

    public DynamoDbFeedbackGateway(DynamoDbClient client,
            @ConfigProperty(name = "feedback.table") String tableName) {
        this.client = client;
        this.tableName = tableName;
    }

    @Override
    public void save(Feedback feedback) {
        client.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(toItem(feedback))
                .build());
    }

    static Map<String, AttributeValue> toItem(Feedback feedback) {
        return Map.of(
                "id", AttributeValue.fromS(feedback.getId().toString()),
                "description", AttributeValue.fromS(feedback.getDescription()),
                "rating", AttributeValue.fromN(Integer.toString(feedback.getRating())),
                "urgency", AttributeValue.fromS(feedback.getUrgency().name()),
                "createdAt", AttributeValue.fromS(CREATED_AT_FORMAT.format(feedback.getCreatedAt())),
                "createdDay", AttributeValue.fromS(CREATED_DAY_FORMAT.format(feedback.getCreatedAt())));
    }
}
