package br.com.fiap.feedback.application.notification;

import br.com.fiap.feedback.application.notification.gateway.AdminNotificationGateway;
import br.com.fiap.feedback.domain.feedback.Feedback;

/** Envia o alerta aos administradores. Só feedback crítico gera alerta; o resto é ignorado. */
public class NotifyCriticalFeedbackUseCase {

    private final AdminNotificationGateway notificationGateway;

    public NotifyCriticalFeedbackUseCase(AdminNotificationGateway notificationGateway) {
        if (notificationGateway == null) {
            throw new IllegalArgumentException("notificationGateway é obrigatório");
        }
        this.notificationGateway = notificationGateway;
    }

    /** Retorna true se o alerta foi enviado e false se o feedback não era crítico. */
    public boolean execute(Feedback feedback) {
        if (feedback == null) {
            throw new IllegalArgumentException("feedback é obrigatório");
        }
        if (!feedback.isCritical()) {
            return false;
        }
        notificationGateway.send(CriticalFeedbackAlert.from(feedback));
        return true;
    }
}
