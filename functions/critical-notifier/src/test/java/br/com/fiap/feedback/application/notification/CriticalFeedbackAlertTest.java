package br.com.fiap.feedback.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.Urgency;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CriticalFeedbackAlert")
class CriticalFeedbackAlertTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    @Test
    @DisplayName("Copia descrição, urgência e data de envio do feedback")
    void shouldCreateFromFeedback() {
        var feedback = Feedback.create("Aula sem áudio", 2, Clock.fixed(NOW, ZoneOffset.UTC));

        var alert = CriticalFeedbackAlert.from(feedback);

        assertThat(alert.getDescription()).isEqualTo("Aula sem áudio");
        assertThat(alert.getUrgency()).isEqualTo(Urgency.HIGH);
        assertThat(alert.getSentAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Rejeita criar o alerta a partir de um feedback nulo")
    void shouldRejectNullFeedback() {
        assertThatThrownBy(() -> CriticalFeedbackAlert.from(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("feedback é obrigatório");
    }

    @Test
    @DisplayName("Considera iguais os alertas com os mesmos dados")
    void shouldBeEqualWhenAllFieldsMatch() {
        var alert = new CriticalFeedbackAlert("texto", Urgency.HIGH, NOW);
        var same = new CriticalFeedbackAlert("texto", Urgency.HIGH, NOW);

        assertThat(alert)
                .isEqualTo(alert)
                .isEqualTo(same)
                .hasSameHashCodeAs(same)
                .isNotEqualTo(new CriticalFeedbackAlert("outro", Urgency.HIGH, NOW))
                .isNotEqualTo(new CriticalFeedbackAlert("texto", Urgency.MEDIUM, NOW))
                .isNotEqualTo(new CriticalFeedbackAlert("texto", Urgency.HIGH, NOW.plusSeconds(1)))
                .isNotEqualTo("texto");
        assertThat(alert.toString()).contains("texto", "HIGH");
    }
}
