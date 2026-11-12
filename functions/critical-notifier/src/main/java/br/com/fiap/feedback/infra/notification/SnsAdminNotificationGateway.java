package br.com.fiap.feedback.infra.notification;

import br.com.fiap.feedback.application.notification.CriticalFeedbackAlert;
import br.com.fiap.feedback.application.notification.gateway.AdminNotificationGateway;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/** Envia o e-mail de urgência pelo tópico SNS. Substitui o LoggingAdminNotificationGateway no build do profile lambda. */
@ApplicationScoped
@IfBuildProperty(name = "feedback.aws.enabled", stringValue = "true")
public class SnsAdminNotificationGateway implements AdminNotificationGateway {

    private static final Logger LOG = Logger.getLogger(SnsAdminNotificationGateway.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final SnsClient client;
    private final String topicArn;
    private final ZoneId zone;

    public SnsAdminNotificationGateway(SnsClient client,
            @ConfigProperty(name = "critical.topic.arn") String topicArn,
            @ConfigProperty(name = "notification.zone-id") String zoneId) {
        this.client = client;
        this.topicArn = topicArn;
        this.zone = ZoneId.of(zoneId);
    }

    /** Publica o alerta; se o SNS falhar, a exceção volta para a fila tentar de novo (depois de 3 vezes, DLQ). */
    @Override
    public void send(CriticalFeedbackAlert alert) {
        try {
            client.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject(subject(alert))
                    .message(message(alert, zone))
                    .build());
        } catch (SdkException e) {
            LOG.errorf(e, "Falha ao enviar e-mail de urgência: urgencia=%s dataEnvio=%s",
                    alert.getUrgency(), alert.getSentAt());
            throw e;
        }
        LOG.infof("E-mail de urgência enviado: urgencia=%s dataEnvio=%s", alert.getUrgency(), alert.getSentAt());
    }

    static String subject(CriticalFeedbackAlert alert) {
        return "[Urgência %s] Nova avaliação crítica".formatted(alert.getUrgency());
    }

    static String message(CriticalFeedbackAlert alert, ZoneId zone) {
        return """
                Uma avaliação crítica foi recebida na plataforma de feedback.

                Descrição: %s
                Urgência: %s
                Data de envio: %s (%s)
                """.formatted(alert.getDescription(), alert.getUrgency(),
                DATE_FORMAT.format(alert.getSentAt().atZone(zone)), zone);
    }
}
