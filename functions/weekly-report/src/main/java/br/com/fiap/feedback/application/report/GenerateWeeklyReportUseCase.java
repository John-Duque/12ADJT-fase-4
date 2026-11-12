package br.com.fiap.feedback.application.report;

import br.com.fiap.feedback.application.report.gateway.FeedbackQueryGateway;
import br.com.fiap.feedback.application.report.gateway.ReportDeliveryGateway;
import br.com.fiap.feedback.domain.report.WeeklyReport;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Gera e entrega o relatório dos 7 dias completos anteriores ao dia da execução, no fuso configurado. */
public class GenerateWeeklyReportUseCase {

    static final int PERIOD_DAYS = 7;

    private final FeedbackQueryGateway queryGateway;
    private final ReportDeliveryGateway deliveryGateway;
    private final Clock clock;
    private final ZoneId zone;

    public GenerateWeeklyReportUseCase(FeedbackQueryGateway queryGateway, ReportDeliveryGateway deliveryGateway,
            Clock clock, ZoneId zone) {
        if (queryGateway == null) {
            throw new IllegalArgumentException("queryGateway é obrigatório");
        }
        if (deliveryGateway == null) {
            throw new IllegalArgumentException("deliveryGateway é obrigatório");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock é obrigatório");
        }
        if (zone == null) {
            throw new IllegalArgumentException("zone é obrigatório");
        }
        this.queryGateway = queryGateway;
        this.deliveryGateway = deliveryGateway;
        this.clock = clock;
        this.zone = zone;
    }

    public WeeklyReport execute() {
        var today = LocalDate.now(clock.withZone(zone));
        var startDate = today.minusDays(PERIOD_DAYS);
        var endDate = today.minusDays(1);
        var from = startDate.atStartOfDay(zone).toInstant();
        var to = today.atStartOfDay(zone).toInstant();

        var feedbacks = queryGateway.findCreatedBetween(from, to);
        var report = WeeklyReport.of(startDate, endDate, feedbacks, zone);
        deliveryGateway.deliver(report);
        return report;
    }
}
