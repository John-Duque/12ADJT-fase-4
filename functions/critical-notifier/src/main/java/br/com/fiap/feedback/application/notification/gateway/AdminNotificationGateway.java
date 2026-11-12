package br.com.fiap.feedback.application.notification.gateway;

import br.com.fiap.feedback.application.notification.CriticalFeedbackAlert;

/** Avisa os administradores sobre um feedback crítico. A infra decide o canal (log, e-mail). */
public interface AdminNotificationGateway {

    void send(CriticalFeedbackAlert alert);
}
