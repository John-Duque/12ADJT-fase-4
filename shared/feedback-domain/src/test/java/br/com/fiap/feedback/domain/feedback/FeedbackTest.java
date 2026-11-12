package br.com.fiap.feedback.domain.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Feedback")
class FeedbackTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("Cria o feedback com id, descrição sem espaços nas pontas, urgência e data do relógio")
    void shouldCreateFeedbackWithDerivedFields() {
        var feedback = Feedback.create("  Aula muito boa  ", 9, clock);

        assertThat(feedback.getId()).isNotNull();
        assertThat(feedback.getDescription()).isEqualTo("Aula muito boa");
        assertThat(feedback.getRating()).isEqualTo(9);
        assertThat(feedback.getUrgency()).isEqualTo(Urgency.LOW);
        assertThat(feedback.getCreatedAt()).isEqualTo(NOW);
        assertThat(feedback.isCritical()).isFalse();
    }

    @Test
    @DisplayName("Marca como crítico o feedback com nota baixa")
    void shouldMarkLowRatingAsCritical() {
        var feedback = Feedback.create("Áudio inaudível", 1, clock);

        assertThat(feedback.getUrgency()).isEqualTo(Urgency.HIGH);
        assertThat(feedback.isCritical()).isTrue();
    }

    @DisplayName("Rejeita descrição nula ou em branco")
    @ParameterizedTest(name = "descrição \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void shouldRejectBlankDescription(String description) {
        assertThatThrownBy(() -> Feedback.create(description, 5, clock))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessageContaining("descrição é obrigatória");
    }

    @Test
    @DisplayName("Rejeita descrição com mais de 1000 caracteres")
    void shouldRejectDescriptionLongerThanLimit() {
        var description = "a".repeat(Feedback.MAX_DESCRIPTION_LENGTH + 1);

        assertThatThrownBy(() -> Feedback.create(description, 5, clock))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessageContaining("no máximo 1000 caracteres");
    }

    @Test
    @DisplayName("Rejeita nota fora do intervalo de 0 a 10")
    void shouldRejectRatingOutOfRange() {
        assertThatThrownBy(() -> Feedback.create("texto", 11, clock))
                .isInstanceOf(InvalidFeedbackException.class);
    }

    @Test
    @DisplayName("Rejeita urgência que não corresponde à nota")
    void shouldRejectUrgencyInconsistentWithRating() {
        var id = UUID.randomUUID();

        assertThatThrownBy(() -> new Feedback(id, "texto", 10, Urgency.HIGH, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rejeita id, data de envio ou relógio nulos")
    void shouldRejectMissingRequiredFields() {
        var id = UUID.randomUUID();

        assertThatThrownBy(() -> new Feedback(null, "texto", 10, Urgency.LOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id é obrigatório");
        assertThatThrownBy(() -> new Feedback(id, "texto", 10, Urgency.LOW, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("createdAt é obrigatório");
        assertThatThrownBy(() -> Feedback.create("texto", 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clock é obrigatório");
    }

    @Test
    @DisplayName("Considera iguais os feedbacks com o mesmo id")
    void shouldBeEqualById() {
        var id = UUID.randomUUID();
        var feedback = new Feedback(id, "texto", 10, Urgency.LOW, NOW);
        var sameId = new Feedback(id, "outro texto", 2, Urgency.HIGH, NOW);
        var otherId = new Feedback(UUID.randomUUID(), "texto", 10, Urgency.LOW, NOW);

        assertThat(feedback)
                .isEqualTo(feedback)
                .isEqualTo(sameId)
                .hasSameHashCodeAs(sameId)
                .isNotEqualTo(otherId)
                .isNotEqualTo("texto");
        assertThat(feedback.toString()).contains(id.toString(), "texto");
    }
}
