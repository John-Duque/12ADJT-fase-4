package br.com.fiap.feedback.infra.config;

import br.com.fiap.feedback.application.report.GenerateWeeklyReportUseCase;
import br.com.fiap.feedback.application.report.gateway.FeedbackQueryGateway;
import br.com.fiap.feedback.application.report.gateway.ReportDeliveryGateway;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.ZoneId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Monta os casos de uso com CDI, para as classes da application continuarem sem anotações. */
public class UseCaseConfig {

    @Produces
    @Singleton
    @DefaultBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** O fuso que fecha os dias do relatório vem de report.zone-id. */
    @Produces
    @Singleton
    public GenerateWeeklyReportUseCase generateWeeklyReportUseCase(FeedbackQueryGateway queryGateway,
            ReportDeliveryGateway deliveryGateway, Clock clock,
            @ConfigProperty(name = "report.zone-id") String zoneId) {
        return new GenerateWeeklyReportUseCase(queryGateway, deliveryGateway, clock, ZoneId.of(zoneId));
    }
}
