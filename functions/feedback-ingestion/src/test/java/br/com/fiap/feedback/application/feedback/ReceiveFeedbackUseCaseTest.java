package br.com.fiap.feedback.application.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.InvalidFeedbackException;
import br.com.fiap.feedback.domain.feedback.Urgency;
import br.com.fiap.feedback.domain.feedback.event.CriticalFeedbackEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReceiveFeedbackUseCase")
class ReceiveFeedbackUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final List<Feedback> saved = new ArrayList<>();
    private final List<CriticalFeedbackEvent> published = new ArrayList<>();
    private final ReceiveFeedbackUseCase useCase = new ReceiveFeedbackUseCase(saved::add, published::add, clock);

    @Test
    @DisplayName("Grava o feedback não crítico sem publicar")
    void shouldSaveNonCriticalFeedbackWithoutPublishing() {
        var feedback = useCase.execute(new ReceiveFeedbackInput("Ótima aula", 9));

        assertThat(saved).containsExactly(feedback);
        assertThat(published).isEmpty();
        assertThat(feedback.getUrgency()).isEqualTo(Urgency.LOW);
        assertThat(feedback.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Grava e publica o feedback crítico")
    void shouldSaveAndPublishCriticalFeedback() {
        var feedback = useCase.execute(new ReceiveFeedbackInput("Vídeo não carrega", 1));

        assertThat(saved).containsExactly(feedback);
        assertThat(published).containsExactly(CriticalFeedbackEvent.from(feedback));
    }

    @Test
    @DisplayName("Rejeita feedback sem nota")
    void shouldRejectMissingRating() {
        var input = new ReceiveFeedbackInput("Sem nota", null);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessage("A nota é obrigatória");
        assertThat(saved).isEmpty();
    }

    @Test
    @DisplayName("Rejeita entrada nula")
    void shouldRejectNullInput() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessage("A nota é obrigatória");
    }

    @Test
    @DisplayName("Não grava nem publica feedback com nota inválida")
    void shouldNotSaveInvalidFeedback() {
        var input = new ReceiveFeedbackInput("Nota inválida", 11);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InvalidFeedbackException.class);
        assertThat(saved).isEmpty();
        assertThat(published).isEmpty();
    }

    @Test
    @DisplayName("Rejeita dependências nulas no construtor")
    void shouldRejectNullDependencies() {
        assertThatThrownBy(() -> new ReceiveFeedbackUseCase(null, published::add, clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("feedbackGateway é obrigatório");
        assertThatThrownBy(() -> new ReceiveFeedbackUseCase(saved::add, null, clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventGateway é obrigatório");
        assertThatThrownBy(() -> new ReceiveFeedbackUseCase(saved::add, published::add, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clock é obrigatório");
    }
}
