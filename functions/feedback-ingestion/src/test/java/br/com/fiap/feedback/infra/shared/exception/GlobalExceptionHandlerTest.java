package br.com.fiap.feedback.infra.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.domain.feedback.InvalidFeedbackException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("Converte exceção de domínio em 400 com a mensagem")
    void shouldMapDomainExceptionToBadRequest() {
        var response = new GlobalExceptionHandler().handleDomainException(new InvalidFeedbackException("x"));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity().message()).isEqualTo("x");
    }
}
