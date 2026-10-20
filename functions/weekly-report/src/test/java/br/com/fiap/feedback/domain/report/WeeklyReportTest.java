package br.com.fiap.feedback.domain.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.Urgency;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WeeklyReport")
class WeeklyReportTest {

    private static final LocalDate START = LocalDate.parse("2026-10-06");
    private static final LocalDate END = LocalDate.parse("2026-10-12");
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Test
    @DisplayName("Agrega total, média, contagem por dia e por urgência e ordena as avaliações")
    void shouldAggregateFeedbacks() {
        var late = feedback("Aula boa", 8, "2026-10-06T15:00:00Z");
        var middle = feedback("Aula ok", 5, "2026-10-10T12:00:00Z");
        var early = feedback("Sem áudio", 2, "2026-10-06T12:00:00Z");

        var report = WeeklyReport.of(START, END, List.of(middle, late, early), ZONE);

        assertThat(report.getStartDate()).isEqualTo(START);
        assertThat(report.getEndDate()).isEqualTo(END);
        assertThat(report.getTotalFeedbacks()).isEqualTo(3);
        assertThat(report.getAverageRating()).isEqualTo(5.0);
        assertThat(report.getCountByDay())
                .contains(entry(START, 2L), entry(LocalDate.parse("2026-10-10"), 1L),
                        entry(LocalDate.parse("2026-10-08"), 0L));
        assertThat(report.getCountByUrgency())
                .containsOnly(entry(Urgency.HIGH, 1L), entry(Urgency.MEDIUM, 1L), entry(Urgency.LOW, 1L));
        assertThat(report.getFeedbacks())
                .extracting(WeeklyReport.Entry::getDescription)
                .containsExactly("Sem áudio", "Aula boa", "Aula ok");
        assertThat(report.getFeedbacks().getFirst().getUrgency()).isEqualTo(Urgency.HIGH);
        assertThat(report.getFeedbacks().getFirst().getCreatedAt()).isEqualTo(Instant.parse("2026-10-06T12:00:00Z"));
    }

    @Test
    @DisplayName("Conta o feedback no dia do fuso configurado")
    void shouldGroupByDayInConfiguredZone() {
        var feedback = feedback("Ótima aula", 9, "2026-10-07T02:00:00Z");

        var report = WeeklyReport.of(START, END, List.of(feedback), ZONE);

        assertThat(report.getCountByDay())
                .containsEntry(START, 1L)
                .containsEntry(LocalDate.parse("2026-10-07"), 0L);
    }

    @Test
    @DisplayName("Arredonda a média para duas casas")
    void shouldRoundAverageToTwoDecimals() {
        var feedbacks = List.of(
                feedback("a", 10, "2026-10-06T12:00:00Z"),
                feedback("b", 10, "2026-10-07T12:00:00Z"),
                feedback("c", 9, "2026-10-08T12:00:00Z"));

        var report = WeeklyReport.of(START, END, feedbacks, ZONE);

        assertThat(report.getAverageRating()).isEqualTo(9.67);
    }

    @Test
    @DisplayName("Gera relatório zerado quando não há avaliações")
    void shouldReturnZeroedReportWhenThereAreNoFeedbacks() {
        var report = WeeklyReport.of(START, END, List.of(), ZONE);

        assertThat(report.getTotalFeedbacks()).isZero();
        assertThat(report.getAverageRating()).isZero();
        assertThat(report.getCountByDay()).hasSize(7).allSatisfy((day, count) -> assertThat(count).isZero());
        assertThat(report.getCountByDay().keySet()).first().isEqualTo(START);
        assertThat(report.getCountByUrgency()).hasSize(3).allSatisfy((urgency, count) -> assertThat(count).isZero());
        assertThat(report.getFeedbacks()).isEmpty();
    }

    @Test
    @DisplayName("Rejeita período com fim antes do início")
    void shouldRejectInvertedPeriod() {
        assertThatThrownBy(() -> WeeklyReport.of(END, START, List.of(), ZONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endDate deve ser igual ou posterior a startDate");
    }

    @Test
    @DisplayName("Rejeita parâmetros nulos")
    void shouldRejectNullArguments() {
        List<Feedback> none = List.of();

        assertThatThrownBy(() -> WeeklyReport.of(null, END, none, ZONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startDate é obrigatório");
        assertThatThrownBy(() -> WeeklyReport.of(START, null, none, ZONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endDate é obrigatório");
        assertThatThrownBy(() -> WeeklyReport.of(START, END, null, ZONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("feedbacks é obrigatório");
        assertThatThrownBy(() -> WeeklyReport.of(START, END, none, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("zone é obrigatório");
    }

    private static Feedback feedback(String description, int rating, String instant) {
        return Feedback.create(description, rating, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }
}
