package br.com.fiap.feedback.infra.persistence;

import br.com.fiap.feedback.application.report.gateway.FeedbackQueryGateway;
import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.Urgency;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

/** Lê os feedbacks do período no índice por-dia do DynamoDB. Substitui o NoOpFeedbackQueryGateway no build do profile lambda. */
@ApplicationScoped
@IfBuildProperty(name = "feedback.aws.enabled", stringValue = "true")
public class DynamoDbFeedbackQueryGateway implements FeedbackQueryGateway {

    private static final Logger LOG = Logger.getLogger(DynamoDbFeedbackQueryGateway.class);

    /** Mesmo formato gravado pelo feedback-ingestion: tamanho fixo, para comparar datas como texto. */
    static final DateTimeFormatter CREATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'").withZone(ZoneOffset.UTC);

    private final DynamoDbClient client;
    private final String tableName;
    private final String dayIndexName;

    public DynamoDbFeedbackQueryGateway(DynamoDbClient client,
            @ConfigProperty(name = "feedback.table") String tableName,
            @ConfigProperty(name = "feedback.day-index") String dayIndexName) {
        this.client = client;
        this.tableName = tableName;
        this.dayIndexName = dayIndexName;
    }

    /** Faz uma consulta por dia UTC do intervalo (partição do índice), limitando createdAt às bordas. */
    @Override
    public List<Feedback> findCreatedBetween(Instant startInclusive, Instant endExclusive) {
        var feedbacks = new ArrayList<Feedback>();
        if (!startInclusive.isBefore(endExclusive)) {
            return feedbacks;
        }
        var lastInstant = endExclusive.minusNanos(1);
        var from = AttributeValue.fromS(CREATED_AT_FORMAT.format(startInclusive));
        var to = AttributeValue.fromS(CREATED_AT_FORMAT.format(lastInstant));
        var lastDay = LocalDate.ofInstant(lastInstant, ZoneOffset.UTC);
        for (var day = LocalDate.ofInstant(startInclusive, ZoneOffset.UTC); !day.isAfter(lastDay); day = day.plusDays(1)) {
            var request = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName(dayIndexName)
                    .keyConditionExpression("createdDay = :day AND createdAt BETWEEN :from AND :to")
                    .expressionAttributeValues(Map.of(
                            ":day", AttributeValue.fromS(day.toString()),
                            ":from", from,
                            ":to", to))
                    .build();
            client.queryPaginator(request).items().forEach(item -> feedbacks.add(toFeedback(item)));
        }
        LOG.debugf("Feedbacks lidos do banco: %d (de %s até %s)", feedbacks.size(), startInclusive, endExclusive);
        return feedbacks;
    }

    static Feedback toFeedback(Map<String, AttributeValue> item) {
        return new Feedback(
                UUID.fromString(item.get("id").s()),
                item.get("description").s(),
                Integer.parseInt(item.get("rating").n()),
                Urgency.valueOf(item.get("urgency").s()),
                Instant.parse(item.get("createdAt").s()));
    }
}
