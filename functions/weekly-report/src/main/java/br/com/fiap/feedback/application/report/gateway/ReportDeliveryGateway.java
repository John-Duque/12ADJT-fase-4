package br.com.fiap.feedback.application.report.gateway;

import br.com.fiap.feedback.domain.report.WeeklyReport;

/** Entrega o relatório aos administradores. A infra decide o canal (log, e-mail). */
public interface ReportDeliveryGateway {

    void deliver(WeeklyReport report);
}
