package br.com.fiap.feedback.infra.config;

import br.com.fiap.feedback.application.notification.NotifyCriticalFeedbackUseCase;
import br.com.fiap.feedback.application.notification.gateway.AdminNotificationGateway;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/** Monta os casos de uso com CDI, para as classes da application continuarem sem anotações. */
public class UseCaseConfig {

    @Produces
    @Singleton
    public NotifyCriticalFeedbackUseCase notifyCriticalFeedbackUseCase(AdminNotificationGateway notificationGateway) {
        return new NotifyCriticalFeedbackUseCase(notificationGateway);
    }
}
