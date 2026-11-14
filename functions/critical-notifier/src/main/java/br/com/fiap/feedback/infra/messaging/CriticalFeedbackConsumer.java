package br.com.fiap.feedback.infra.messaging;

import br.com.fiap.feedback.application.notification.NotifyCriticalFeedbackUseCase;
import br.com.fiap.feedback.domain.feedback.event.CriticalFeedbackEvent;
import io.quarkus.funqy.Funq;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/** Função acionada pela fila de críticos: converte o evento em Feedback e dispara a notificação. */
public class CriticalFeedbackConsumer {

    private static final Logger LOG = Logger.getLogger(CriticalFeedbackConsumer.class);

    @Inject
    NotifyCriticalFeedbackUseCase notifyCriticalFeedbackUseCase;

    @Funq("notify-critical-feedback")
    public NotificationResult consume(CriticalFeedbackEvent event) {
        try {
            var notified = notifyCriticalFeedbackUseCase.execute(event.toFeedback());
            if (notified) {
                LOG.infof("Feedback crítico notificado: id=%s", event.getId());
            } else {
                LOG.warnf("Evento ignorado: feedback %s não é crítico (urgencia=%s)", event.getId(), event.getUrgency());
            }
            return new NotificationResult(event.getId(), notified);
        } catch (RuntimeException e) {
            // Loga o id para rastrear a falha; a exceção segue para a mensagem voltar à fila
            LOG.errorf(e, "Falha ao notificar feedback crítico: id=%s", event.getId());
            throw e;
        }
    }
}
