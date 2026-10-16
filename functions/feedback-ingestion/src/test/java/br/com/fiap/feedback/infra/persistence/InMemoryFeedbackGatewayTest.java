package br.com.fiap.feedback.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.domain.feedback.Feedback;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryFeedbackGateway")
class InMemoryFeedbackGatewayTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryFeedbackGateway gateway = new InMemoryFeedbackGateway();

    @Test
    @DisplayName("Grava os feedbacks e lista todos")
    void shouldSaveAndList() {
        var first = Feedback.create("Ótima aula", 9, clock);
        var second = Feedback.create("Sem áudio", 1, clock);

        gateway.save(first);
        gateway.save(second);

        assertThat(gateway.findAll()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    @DisplayName("Mantém um único registro por id")
    void shouldKeepOnePerId() {
        var feedback = Feedback.create("Ótima aula", 9, clock);

        gateway.save(feedback);
        gateway.save(feedback);

        assertThat(gateway.findAll()).containsExactly(feedback);
    }
}
