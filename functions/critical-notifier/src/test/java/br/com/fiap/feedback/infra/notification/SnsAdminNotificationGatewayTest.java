package br.com.fiap.feedback.infra.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.application.notification.CriticalFeedbackAlert;
import br.com.fiap.feedback.domain.feedback.Urgency;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

@DisplayName("SnsAdminNotificationGateway")
class SnsAdminNotificationGatewayTest {

    private static final String TOPIC_ARN = "arn:aws:sns:us-east-2:000000000000:feedbacks-criticos";
    private static final CriticalFeedbackAlert ALERT =
            new CriticalFeedbackAlert("Aula sem áudio", Urgency.HIGH, Instant.parse("2026-10-13T12:00:00Z"));

    private final List<PublishRequest> requests = new ArrayList<>();

    @Test
    @DisplayName("Publica no tópico configurado o e-mail com descrição, urgência e data de envio")
    void shouldPublishAlertToTopic() {
        var gateway = new SnsAdminNotificationGateway(new FakeSnsClient(false), TOPIC_ARN, "America/Sao_Paulo");

        gateway.send(ALERT);

        assertThat(requests).hasSize(1);
        var request = requests.get(0);
        assertThat(request.topicArn()).isEqualTo(TOPIC_ARN);
        assertThat(request.subject()).isEqualTo("[Urgência HIGH] Nova avaliação crítica");
        assertThat(request.message())
                .contains("Descrição: Aula sem áudio")
                .contains("Urgência: HIGH")
                .contains("Data de envio: 13/10/2026 09:00:00 (America/Sao_Paulo)");
    }

    @Test
    @DisplayName("Repassa a falha do SNS para a mensagem voltar para a fila")
    void shouldRethrowSnsFailure() {
        var gateway = new SnsAdminNotificationGateway(new FakeSnsClient(true), TOPIC_ARN, "America/Sao_Paulo");

        assertThatThrownBy(() -> gateway.send(ALERT)).isInstanceOf(SnsException.class);
        assertThat(requests).isEmpty();
    }

    private class FakeSnsClient implements SnsClient {

        private final boolean failing;

        FakeSnsClient(boolean failing) {
            this.failing = failing;
        }

        @Override
        public PublishResponse publish(PublishRequest request) {
            if (failing) {
                throw SnsException.builder().message("tópico indisponível").build();
            }
            requests.add(request);
            return PublishResponse.builder().messageId("1").build();
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
