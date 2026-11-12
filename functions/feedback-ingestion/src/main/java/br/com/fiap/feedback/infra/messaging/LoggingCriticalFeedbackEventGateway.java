package br.com.fiap.feedback.infra.messaging;

import br.com.fiap.feedback.application.feedback.gateway.CriticalFeedbackEventGateway;
import br.com.fiap.feedback.domain.feedback.event.CriticalFeedbackEvent;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/** Só loga o evento crítico até existir a fila. Sai de cena quando houver outro CriticalFeedbackEventGateway. */
@DefaultBean
@ApplicationScoped
public class LoggingCriticalFeedbackEventGateway implements CriticalFeedbackEventGateway {

    private static final Logger LOG = Logger.getLogger(LoggingCriticalFeedbackEventGateway.class);

    @Override
    public void publish(CriticalFeedbackEvent event) {
        LOG.infof("Feedback crítico publicado: id=%s urgencia=%s", event.getId(), event.getUrgency());
    }
}
