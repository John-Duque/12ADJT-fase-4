package br.com.fiap.feedback.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.application.notification.NotifyCriticalFeedbackUseCase;
import br.com.fiap.feedback.domain.feedback.Feedback;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisplayName("UseCaseConfig")
class UseCaseConfigTest {

    @Inject
    NotifyCriticalFeedbackUseCase useCase;

    @Test
    @DisplayName("Monta o caso de uso com o gateway de notificação provisório")
    void shouldWireUseCase() {
        var notified = useCase.execute(Feedback.create("Sem áudio", 1, Clock.systemUTC()));

        assertThat(notified).isTrue();
    }
}
