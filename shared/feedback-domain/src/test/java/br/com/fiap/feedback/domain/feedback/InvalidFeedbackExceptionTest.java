package br.com.fiap.feedback.domain.feedback;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.domain.shared.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidFeedbackException")
class InvalidFeedbackExceptionTest {

    @Test
    @DisplayName("Mantém a mensagem recebida no construtor")
    void shouldKeepMessage() {
        var exception = new InvalidFeedbackException("mensagem");

        assertThat(exception.getMessage()).isEqualTo("mensagem");
    }

    @Test
    @DisplayName("É uma DomainException e uma RuntimeException")
    void shouldBeDomainException() {
        var exception = new InvalidFeedbackException("mensagem");

        assertThat(exception)
                .isInstanceOf(DomainException.class)
                .isInstanceOf(RuntimeException.class);
    }
}
