package br.com.fiap.feedback.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.application.report.gateway.FeedbackQueryGateway;
import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.report.WeeklyReport;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GenerateWeeklyReportUseCase")
class GenerateWeeklyReportUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final List<Instant> queriedRange = new ArrayList<>();
    private final List<WeeklyReport> delivered = new ArrayList<>();
    private final Feedback feedback =
            Feedback.create("Aula boa", 8, Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneOffset.UTC));
    private final FeedbackQueryGateway queryGateway = (from, to) -> {
        queriedRange.add(from);
        queriedRange.add(to);
        return List.of(feedback);
    };

    @Test
    @DisplayName("Consulta os 7 dias completos anteriores no fuso e entrega o relatório")
    void shouldQueryLastSevenFullDaysAndDeliverReport() {
        var useCase = new GenerateWeeklyReportUseCase(queryGateway, delivered::add, clock, ZONE);

        var report = useCase.execute();

        assertThat(queriedRange)
                .containsExactly(Instant.parse("2026-10-06T03:00:00Z"), Instant.parse("2026-10-13T03:00:00Z"));
        assertThat(report.getStartDate()).isEqualTo(LocalDate.parse("2026-10-06"));
        assertThat(report.getEndDate()).isEqualTo(LocalDate.parse("2026-10-12"));
        assertThat(report.getTotalFeedbacks()).isEqualTo(1);
        assertThat(delivered).containsExactly(report);
    }

    @Test
    @DisplayName("Rejeita dependências nulas no construtor")
    void shouldRejectNullDependencies() {
        assertThatThrownBy(() -> new GenerateWeeklyReportUseCase(null, delivered::add, clock, ZONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queryGateway é obrigatório");
        assertThatThrownBy(() -> new GenerateWeeklyReportUseCase(queryGateway, null, clock, ZONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("deliveryGateway é obrigatório");
        assertThatThrownBy(() -> new GenerateWeeklyReportUseCase(queryGateway, delivered::add, null, ZONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clock é obrigatório");
        assertThatThrownBy(() -> new GenerateWeeklyReportUseCase(queryGateway, delivered::add, clock, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("zone é obrigatório");
    }
}
