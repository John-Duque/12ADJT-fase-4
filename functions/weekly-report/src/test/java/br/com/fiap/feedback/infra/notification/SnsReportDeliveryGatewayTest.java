package br.com.fiap.feedback.infra.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.Urgency;
import br.com.fiap.feedback.domain.report.WeeklyReport;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

@DisplayName("SnsReportDeliveryGateway")
class SnsReportDeliveryGatewayTest {

    private static final String TOPIC_ARN = "arn:aws:sns:us-east-2:000000000000:relatorio-semanal";
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate START = LocalDate.parse("2026-10-06");
    private static final LocalDate END = LocalDate.parse("2026-10-12");

    private final List<PublishRequest> requests = new ArrayList<>();

    @Test
    @DisplayName("Publica no tópico o relatório com média, contagens por urgência e por dia e a lista de avaliações")
    void shouldPublishReportWithAllRequiredData() {
        var report = WeeklyReport.of(START, END, List.of(
                feedback("Aula sem áudio", 1, "2026-10-06T12:00:00Z"),
                feedback("Aula média", 5, "2026-10-07T15:00:00Z"),
                feedback("Aula ótima", 9, "2026-10-07T18:30:00Z")), ZONE);
        var gateway = new SnsReportDeliveryGateway(new FakeSnsClient(false), TOPIC_ARN, "America/Sao_Paulo");

        gateway.deliver(report);

        assertThat(requests).hasSize(1);
        var request = requests.get(0);
        assertThat(request.topicArn()).isEqualTo(TOPIC_ARN);
        assertThat(request.subject()).isEqualTo("Relatório semanal de avaliações: 06/10/2026 a 12/10/2026");
        assertThat(request.message())
                .contains("Período: 06/10/2026 a 12/10/2026 (America/Sao_Paulo)")
                .contains("Total de avaliações: 3")
                .contains("Média das notas: 5,00")
                .contains("- HIGH: 1\n- MEDIUM: 1\n- LOW: 1")
                .contains("- 06/10/2026: 1\n- 07/10/2026: 2\n- 08/10/2026: 0")
                .contains("- 06/10/2026 09:00:00 | HIGH | Aula sem áudio")
                .contains("- 07/10/2026 15:30:00 | LOW | Aula ótima");
    }

    @Test
    @DisplayName("Relatório sem avaliações avisa que não houve avaliações no período")
    void shouldTellWhenThereAreNoFeedbacks() {
        var message = SnsReportDeliveryGateway.message(WeeklyReport.of(START, END, List.of(), ZONE), ZONE);

        assertThat(message).contains("Total de avaliações: 0").contains("Nenhuma avaliação no período.");
    }

    @Test
    @DisplayName("Corta a lista de avaliações para a mensagem caber no limite do SNS")
    void shouldCutListToFitSnsLimit() {
        var longDescription = "x".repeat(Feedback.MAX_DESCRIPTION_LENGTH);
        var feedbacks = IntStream.range(0, 300)
                .mapToObj(i -> feedback(longDescription, 8, "2026-10-08T12:00:00Z"))
                .toList();

        var message = SnsReportDeliveryGateway.message(WeeklyReport.of(START, END, feedbacks, ZONE), ZONE);

        assertThat(message.getBytes(StandardCharsets.UTF_8)).hasSizeLessThanOrEqualTo(SnsReportDeliveryGateway.MAX_MESSAGE_BYTES + 200);
        assertThat(message).containsPattern("\\.\\.\\. e mais \\d+ avaliações");
    }

    @Test
    @DisplayName("Repassa a falha do SNS para a execução falhar")
    void shouldRethrowSnsFailure() {
        var gateway = new SnsReportDeliveryGateway(new FakeSnsClient(true), TOPIC_ARN, "America/Sao_Paulo");
        var report = WeeklyReport.of(START, END, List.of(), ZONE);

        assertThatThrownBy(() -> gateway.deliver(report)).isInstanceOf(SnsException.class);
    }

    private static Feedback feedback(String description, int rating, String createdAt) {
        return new Feedback(UUID.randomUUID(), description, rating, Urgency.fromRating(rating), Instant.parse(createdAt));
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
