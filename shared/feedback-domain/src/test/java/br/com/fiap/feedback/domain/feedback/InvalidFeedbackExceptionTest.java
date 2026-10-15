package br.com.fiap.feedback.domain.feedback;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.domain.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

class InvalidFeedbackExceptionTest {

    @Test
    void shouldKeepMessage() {
        var exception = new InvalidFeedbackException("mensagem");

        assertThat(exception.getMessage()).isEqualTo("mensagem");
    }

    @Test
    void shouldBeDomainException() {
        var exception = new InvalidFeedbackException("mensagem");

        assertThat(exception)
                .isInstanceOf(DomainException.class)
                .isInstanceOf(RuntimeException.class);
    }
}
