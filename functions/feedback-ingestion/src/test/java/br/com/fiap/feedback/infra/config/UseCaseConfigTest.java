package br.com.fiap.feedback.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.application.feedback.ReceiveFeedbackInput;
import br.com.fiap.feedback.application.feedback.ReceiveFeedbackUseCase;
import br.com.fiap.feedback.infra.persistence.InMemoryFeedbackGateway;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisplayName("UseCaseConfig")
class UseCaseConfigTest {

    @Inject
    ReceiveFeedbackUseCase useCase;

    @Inject
    InMemoryFeedbackGateway gateway;

    @Test
    @DisplayName("Monta o caso de uso com os gateways provisórios")
    void shouldWireUseCase() {
        var feedback = useCase.execute(new ReceiveFeedbackInput("Sem áudio", 1));

        assertThat(gateway.findAll()).contains(feedback);
    }
}
