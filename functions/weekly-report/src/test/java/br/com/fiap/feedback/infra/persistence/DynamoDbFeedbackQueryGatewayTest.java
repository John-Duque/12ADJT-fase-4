package br.com.fiap.feedback.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.Urgency;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

@DisplayName("DynamoDbFeedbackQueryGateway")
class DynamoDbFeedbackQueryGatewayTest {

    private static final Instant FROM = Instant.parse("2026-10-06T03:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-13T03:00:00Z");

    private final List<QueryRequest> requests = new ArrayList<>();
    private final Map<String, List<List<Map<String, AttributeValue>>>> pagesByDay = new HashMap<>();
    private final DynamoDbFeedbackQueryGateway gateway =
            new DynamoDbFeedbackQueryGateway(new FakeDynamoDbClient(), "feedbacks", "por-dia");

    @Test
    @DisplayName("Consulta o índice por-dia uma vez para cada dia UTC do intervalo, com as bordas de createdAt")
    void shouldQueryEachUtcDayOfInterval() {
        gateway.findCreatedBetween(FROM, TO);

        assertThat(requests).extracting(request -> request.expressionAttributeValues().get(":day").s())
                .containsExactly("2026-10-06", "2026-10-07", "2026-10-08", "2026-10-09",
                        "2026-10-10", "2026-10-11", "2026-10-12", "2026-10-13");
        var request = requests.get(0);
        assertThat(request.tableName()).isEqualTo("feedbacks");
        assertThat(request.indexName()).isEqualTo("por-dia");
        assertThat(request.keyConditionExpression()).isEqualTo("createdDay = :day AND createdAt BETWEEN :from AND :to");
        assertThat(request.expressionAttributeValues().get(":from").s()).isEqualTo("2026-10-06T03:00:00.000000000Z");
        assertThat(request.expressionAttributeValues().get(":to").s()).isEqualTo("2026-10-13T02:59:59.999999999Z");
    }

    @Test
    @DisplayName("Converte os itens de todos os dias e de todas as páginas em Feedback")
    void shouldMapItemsFromAllDaysAndPages() {
        var first = item("8f14e45f-ceea-467a-9b8e-6c2a9b8f1a11", "Aula sem áudio", 1, "HIGH", "2026-10-06T12:00:00.000000000Z");
        var second = item("3c6e0b8a-9c0b-4b1e-8f7a-2d5e6f7a8b9c", "Aula boa", 8, "LOW", "2026-10-06T15:30:00.500000000Z");
        var third = item("0b7d2f1e-1a2b-4c3d-8e9f-a0b1c2d3e4f5", "Aula média", 5, "MEDIUM", "2026-10-10T09:00:00.000000000Z");
        pagesByDay.put("2026-10-06", List.of(List.of(first), List.of(second)));
        pagesByDay.put("2026-10-10", List.of(List.of(third)));

        var feedbacks = gateway.findCreatedBetween(FROM, TO);

        assertThat(feedbacks).extracting(Feedback::getDescription).containsExactly("Aula sem áudio", "Aula boa", "Aula média");
        var feedback = feedbacks.get(1);
        assertThat(feedback.getId()).isEqualTo(UUID.fromString("3c6e0b8a-9c0b-4b1e-8f7a-2d5e6f7a8b9c"));
        assertThat(feedback.getRating()).isEqualTo(8);
        assertThat(feedback.getUrgency()).isEqualTo(Urgency.LOW);
        assertThat(feedback.getCreatedAt()).isEqualTo(Instant.parse("2026-10-06T15:30:00.5Z"));
    }

    @Test
    @DisplayName("Intervalo vazio não consulta o banco")
    void shouldNotQueryEmptyInterval() {
        assertThat(gateway.findCreatedBetween(TO, TO)).isEmpty();
        assertThat(requests).isEmpty();
    }

    private static Map<String, AttributeValue> item(String id, String description, int rating, String urgency,
            String createdAt) {
        return Map.of(
                "id", AttributeValue.fromS(id),
                "description", AttributeValue.fromS(description),
                "rating", AttributeValue.fromN(Integer.toString(rating)),
                "urgency", AttributeValue.fromS(urgency),
                "createdAt", AttributeValue.fromS(createdAt),
                "createdDay", AttributeValue.fromS(createdAt.substring(0, 10)));
    }

    /** Devolve as páginas cadastradas por dia; a chave de continuação guarda o número da próxima página. */
    private class FakeDynamoDbClient implements DynamoDbClient {

        @Override
        public QueryResponse query(QueryRequest request) {
            requests.add(request);
            var day = request.expressionAttributeValues().get(":day").s();
            var pages = pagesByDay.getOrDefault(day, List.of(List.of()));
            var page = request.hasExclusiveStartKey() ? Integer.parseInt(request.exclusiveStartKey().get("page").n()) : 0;
            var response = QueryResponse.builder().items(pages.get(page)).count(pages.get(page).size());
            if (page + 1 < pages.size()) {
                response.lastEvaluatedKey(Map.of("page", AttributeValue.fromN(Integer.toString(page + 1))));
            }
            return response.build();
        }

        @Override
        public String serviceName() {
            return SERVICE_NAME;
        }

        @Override
        public void close() {
            // nada a liberar no cliente falso
        }
    }
}
