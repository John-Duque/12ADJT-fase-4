package br.com.fiap.feedback.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.Urgency;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NotifyCriticalFeedbackUseCase")
class NotifyCriticalFeedbackUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final List<CriticalFeedbackAlert> sent = new ArrayList<>();
    private final NotifyCriticalFeedbackUseCase useCase = new NotifyCriticalFeedbackUseCase(sent::add);

    @Test
    @DisplayName("Envia o alerta para feedback crítico")
    void shouldNotifyCriticalFeedback() {
        var feedback = Feedback.create("Professor não apareceu", 0, clock);

        var notified = useCase.execute(feedback);

        assertThat(notified).isTrue();
        assertThat(sent).containsExactly(new CriticalFeedbackAlert("Professor não apareceu", Urgency.HIGH, NOW));
    }

    @Test
    @DisplayName("Ignora feedback não crítico sem enviar alerta")
    void shouldIgnoreNonCriticalFeedback() {
        var feedback = Feedback.create("Aula ok", 6, clock);

        var notified = useCase.execute(feedback);

        assertThat(notified).isFalse();
        assertThat(sent).isEmpty();
    }

    @Test
    @DisplayName("Rejeita feedback nulo")
    void shouldRejectNullFeedback() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("feedback é obrigatório");
    }

    @Test
    @DisplayName("Rejeita gateway nulo no construtor")
    void shouldRejectNullGateway() {
        assertThatThrownBy(() -> new NotifyCriticalFeedbackUseCase(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("notificationGateway é obrigatório");
    }
}
