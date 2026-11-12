package br.com.fiap.feedback.domain.feedback.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.InvalidFeedbackException;
import br.com.fiap.feedback.domain.feedback.Urgency;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CriticalFeedbackEvent")
class CriticalFeedbackEventTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("Converte o feedback em evento e de volta sem perder dados")
    void shouldConvertFeedbackToEventAndBack() {
        var feedback = Feedback.create("Aula sem áudio", 2, clock);

        var event = CriticalFeedbackEvent.from(feedback);
        var rebuilt = event.toFeedback();

        assertThat(event.getId()).isEqualTo(feedback.getId());
        assertThat(event.getDescription()).isEqualTo("Aula sem áudio");
        assertThat(event.getRating()).isEqualTo(2);
        assertThat(event.getUrgency()).isEqualTo(Urgency.HIGH);
        assertThat(event.getCreatedAt()).isEqualTo(NOW);
        assertThat(rebuilt).isEqualTo(feedback);
        assertThat(rebuilt.getDescription()).isEqualTo(feedback.getDescription());
        assertThat(rebuilt.getRating()).isEqualTo(feedback.getRating());
        assertThat(rebuilt.getUrgency()).isEqualTo(feedback.getUrgency());
        assertThat(rebuilt.getCreatedAt()).isEqualTo(feedback.getCreatedAt());
    }

    @Test
    @DisplayName("Rejeita criar o evento a partir de um feedback nulo")
    void shouldRejectNullFeedback() {
        assertThatThrownBy(() -> CriticalFeedbackEvent.from(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("feedback é obrigatório");
    }

    @Test
    @DisplayName("Reaplica as validações ao reconstruir o feedback")
    void shouldValidateWhenRebuildingFeedback() {
        var event = new CriticalFeedbackEvent(UUID.randomUUID(), " ", 1, Urgency.HIGH, NOW);

        assertThatThrownBy(event::toFeedback)
                .isInstanceOf(InvalidFeedbackException.class);
    }

    @Test
    @DisplayName("Considera iguais os eventos com os mesmos dados")
    void shouldBeEqualWhenAllFieldsMatch() {
        var id = UUID.randomUUID();
        var event = new CriticalFeedbackEvent(id, "texto", 1, Urgency.HIGH, NOW);
        var same = new CriticalFeedbackEvent(id, "texto", 1, Urgency.HIGH, NOW);

        assertThat(event)
                .isEqualTo(event)
                .isEqualTo(same)
                .hasSameHashCodeAs(same)
                .isNotEqualTo(new CriticalFeedbackEvent(UUID.randomUUID(), "texto", 1, Urgency.HIGH, NOW))
                .isNotEqualTo(new CriticalFeedbackEvent(id, "outro", 1, Urgency.HIGH, NOW))
                .isNotEqualTo(new CriticalFeedbackEvent(id, "texto", 2, Urgency.HIGH, NOW))
                .isNotEqualTo(new CriticalFeedbackEvent(id, "texto", 1, Urgency.MEDIUM, NOW))
                .isNotEqualTo(new CriticalFeedbackEvent(id, "texto", 1, Urgency.HIGH, NOW.plusSeconds(1)))
                .isNotEqualTo("texto");
        assertThat(event.toString()).contains(id.toString(), "texto");
    }
}
