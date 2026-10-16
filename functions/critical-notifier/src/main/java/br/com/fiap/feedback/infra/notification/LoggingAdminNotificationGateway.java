package br.com.fiap.feedback.infra.notification;

import br.com.fiap.feedback.application.notification.CriticalFeedbackAlert;
import br.com.fiap.feedback.application.notification.gateway.AdminNotificationGateway;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/** Só loga o alerta até existir o envio de e-mail. Sai de cena quando houver outro AdminNotificationGateway. */
@DefaultBean
@ApplicationScoped
public class LoggingAdminNotificationGateway implements AdminNotificationGateway {

    private static final Logger LOG = Logger.getLogger(LoggingAdminNotificationGateway.class);

    @Override
    public void send(CriticalFeedbackAlert alert) {
        LOG.infof("[ALERTA] urgencia=%s dataEnvio=%s descricao=%s",
                alert.getUrgency(), alert.getSentAt(), alert.getDescription());
    }
}
