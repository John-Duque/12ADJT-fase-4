package br.com.fiap.feedback.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.Urgency;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

@DisplayName("DynamoDbFeedbackGateway")
class DynamoDbFeedbackGatewayTest {

    private static final UUID ID = UUID.fromString("8f14e45f-ceea-467a-9b8e-6c2a9b8f1a11");

    private final List<PutItemRequest> requests = new ArrayList<>();
    private final DynamoDbFeedbackGateway gateway = new DynamoDbFeedbackGateway(new FakeDynamoDbClient(), "feedbacks");

    @Test
    @DisplayName("Grava o feedback com todos os campos na tabela configurada")
    void shouldPutItemWithAllFields() {
        var feedback = new Feedback(ID, "Aula sem áudio", 1, Urgency.HIGH, Instant.parse("2026-10-13T12:00:00Z"));

        gateway.save(feedback);

        assertThat(requests).hasSize(1);
        var request = requests.get(0);
        assertThat(request.tableName()).isEqualTo("feedbacks");
        var item = request.item();
        assertThat(item.get("id").s()).isEqualTo(ID.toString());
        assertThat(item.get("description").s()).isEqualTo("Aula sem áudio");
        assertThat(item.get("rating").n()).isEqualTo("1");
        assertThat(item.get("urgency").s()).isEqualTo("HIGH");
        assertThat(item.get("createdAt").s()).isEqualTo("2026-10-13T12:00:00.000000000Z");
        assertThat(item.get("createdDay").s()).isEqualTo("2026-10-13");
    }

    @Test
    @DisplayName("Usa o dia em UTC e data de tamanho fixo")
    void shouldUseUtcDayAndFixedLengthDate() {
        var feedback = new Feedback(ID, "Ótima aula", 9, Urgency.LOW, Instant.parse("2026-10-13T23:59:59.5Z"));

        var item = DynamoDbFeedbackGateway.toItem(feedback);

        assertThat(item.get("createdAt").s()).isEqualTo("2026-10-13T23:59:59.500000000Z");
        assertThat(item.get("createdDay").s()).isEqualTo("2026-10-13");
    }

    private class FakeDynamoDbClient implements DynamoDbClient {

        @Override
        public PutItemResponse putItem(PutItemRequest request) {
            requests.add(request);
            return PutItemResponse.builder().build();
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
