package br.com.fiap.feedback.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.application.report.GenerateWeeklyReportUseCase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisplayName("UseCaseConfig")
class UseCaseConfigTest {

    @Inject
    GenerateWeeklyReportUseCase useCase;

    @Test
    @DisplayName("Monta o caso de uso com os gateways provisórios e o fuso configurado")
    void shouldWireUseCase() {
        var report = useCase.execute();

        assertThat(report.getCountByDay()).hasSize(7);
        assertThat(report.getTotalFeedbacks()).isZero();
    }
}
