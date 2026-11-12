package br.com.fiap.feedback.infra.notification;

import br.com.fiap.feedback.application.report.gateway.ReportDeliveryGateway;
import br.com.fiap.feedback.domain.report.WeeklyReport;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/** Envia o relatório semanal por e-mail pelo tópico SNS. Substitui o LoggingReportDeliveryGateway no build do profile lambda. */
@ApplicationScoped
@IfBuildProperty(name = "feedback.aws.enabled", stringValue = "true")
public class SnsReportDeliveryGateway implements ReportDeliveryGateway {

    private static final Logger LOG = Logger.getLogger(SnsReportDeliveryGateway.class);
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Locale PT_BR = Locale.of("pt", "BR");

    /** Folga abaixo do limite de 256 KB de uma mensagem do SNS. */
    static final int MAX_MESSAGE_BYTES = 240_000;

    private final SnsClient client;
    private final String topicArn;
    private final ZoneId zone;

    public SnsReportDeliveryGateway(SnsClient client,
            @ConfigProperty(name = "report.topic.arn") String topicArn,
            @ConfigProperty(name = "report.zone-id") String zoneId) {
        this.client = client;
        this.topicArn = topicArn;
        this.zone = ZoneId.of(zoneId);
    }

    /** Publica o relatório; se o SNS falhar, a execução falha e o agendamento tenta de novo. */
    @Override
    public void deliver(WeeklyReport report) {
        try {
            client.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject(subject(report))
                    .message(message(report, zone))
                    .build());
        } catch (SdkException e) {
            LOG.errorf(e, "Falha ao enviar relatório semanal: periodo=%s a %s", report.getStartDate(), report.getEndDate());
            throw e;
        }
        LOG.infof("Relatório semanal enviado: periodo=%s a %s total=%d media=%.2f",
                report.getStartDate(), report.getEndDate(), report.getTotalFeedbacks(), report.getAverageRating());
    }

    static String subject(WeeklyReport report) {
        return "Relatório semanal de avaliações: %s a %s"
                .formatted(DAY_FORMAT.format(report.getStartDate()), DAY_FORMAT.format(report.getEndDate()));
    }

    /** Texto do e-mail com todos os dados pedidos no enunciado; a lista é cortada se passar do limite do SNS. */
    static String message(WeeklyReport report, ZoneId zone) {
        var text = new StringBuilder()
                .append("Relatório semanal de avaliações\n")
                .append("Período: %s a %s (%s)\n\n".formatted(
                        DAY_FORMAT.format(report.getStartDate()), DAY_FORMAT.format(report.getEndDate()), zone))
                .append("Total de avaliações: %d\n".formatted(report.getTotalFeedbacks()))
                .append(String.format(PT_BR, "Média das notas: %.2f%n%n", report.getAverageRating()))
                .append("Quantidade por urgência:\n");
        report.getCountByUrgency().forEach((urgency, count) -> text.append("- %s: %d\n".formatted(urgency, count)));
        text.append("\nQuantidade por dia:\n");
        report.getCountByDay().forEach((day, count) -> text.append("- %s: %d\n".formatted(DAY_FORMAT.format(day), count)));
        text.append("\nAvaliações (data de envio | urgência | descrição):\n");
        if (report.getFeedbacks().isEmpty()) {
            text.append("Nenhuma avaliação no período.\n");
        }
        var entries = report.getFeedbacks();
        var size = text.toString().getBytes(StandardCharsets.UTF_8).length;
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            var line = "- %s | %s | %s\n".formatted(
                    DATE_TIME_FORMAT.format(entry.getCreatedAt().atZone(zone)), entry.getUrgency(), entry.getDescription());
            size += line.getBytes(StandardCharsets.UTF_8).length;
            if (size > MAX_MESSAGE_BYTES) {
                text.append("... e mais %d avaliações (lista cortada pelo limite de tamanho do e-mail)\n"
                        .formatted(entries.size() - i));
                break;
            }
            text.append(line);
        }
        return text.toString();
    }
}
