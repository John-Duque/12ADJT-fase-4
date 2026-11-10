package br.com.fiap.feedback.infra.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.domain.feedback.Urgency;
import br.com.fiap.feedback.domain.feedback.event.CriticalFeedbackEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

@DisplayName("SqsCriticalFeedbackEventGateway")
class SqsCriticalFeedbackEventGatewayTest {

    private static final String QUEUE_URL = "https://sqs.us-east-2.amazonaws.com/000000000000/critical-feedbacks";

    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final List<SendMessageRequest> requests = new ArrayList<>();
    private final SqsCriticalFeedbackEventGateway gateway =
            new SqsCriticalFeedbackEventGateway(new FakeSqsClient(), mapper, QUEUE_URL);

    @Test
    @DisplayName("Publica o evento em JSON na fila configurada, no formato que o notifier lê")
    void shouldSendEventAsJson() throws Exception {
        var event = new CriticalFeedbackEvent(UUID.fromString("8f14e45f-ceea-467a-9b8e-6c2a9b8f1a11"),
                "Aula sem áudio", 1, Urgency.HIGH, Instant.parse("2026-10-13T12:00:00Z"));

        gateway.publish(event);

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).queueUrl()).isEqualTo(QUEUE_URL);
        var body = requests.get(0).messageBody();
        assertThat(body).contains("\"createdAt\":\"2026-10-13T12:00:00Z\"");
        assertThat(mapper.readValue(body, CriticalFeedbackEvent.class)).isEqualTo(event);
    }

    @Test
    @DisplayName("Repassa a falha do SQS para a requisição não terminar como sucesso")
    void shouldRethrowSqsFailure() {
        var failing = new SqsCriticalFeedbackEventGateway(new FakeSqsClient(true), mapper, QUEUE_URL);
        var event = new CriticalFeedbackEvent(UUID.randomUUID(), "Aula sem áudio", 1, Urgency.HIGH,
                Instant.parse("2026-10-13T12:00:00Z"));

        assertThatThrownBy(() -> failing.publish(event)).isInstanceOf(SqsException.class);
    }

    private class FakeSqsClient implements SqsClient {

        private final boolean failing;

        FakeSqsClient() {
            this(false);
        }

        FakeSqsClient(boolean failing) {
            this.failing = failing;
        }

        @Override
        public SendMessageResponse sendMessage(SendMessageRequest request) {
            if (failing) {
                throw SqsException.builder().message("fila indisponível").build();
            }
            requests.add(request);
            return SendMessageResponse.builder().messageId("1").build();
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
