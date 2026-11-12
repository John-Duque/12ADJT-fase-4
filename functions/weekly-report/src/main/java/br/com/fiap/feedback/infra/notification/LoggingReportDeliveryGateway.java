package br.com.fiap.feedback.infra.notification;

import br.com.fiap.feedback.application.report.gateway.ReportDeliveryGateway;
import br.com.fiap.feedback.domain.report.WeeklyReport;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/** Só loga o resumo do relatório até existir o envio de e-mail. Sai de cena quando houver outro ReportDeliveryGateway. */
@DefaultBean
@ApplicationScoped
public class LoggingReportDeliveryGateway implements ReportDeliveryGateway {

    private static final Logger LOG = Logger.getLogger(LoggingReportDeliveryGateway.class);

    @Override
    public void deliver(WeeklyReport report) {
        LOG.infof("Relatório %s a %s: total=%d media=%.2f porUrgencia=%s",
                report.getStartDate(), report.getEndDate(), report.getTotalFeedbacks(), report.getAverageRating(),
                report.getCountByUrgency());
    }
}
