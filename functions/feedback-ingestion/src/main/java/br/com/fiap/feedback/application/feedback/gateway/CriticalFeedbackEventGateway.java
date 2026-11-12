package br.com.fiap.feedback.application.feedback.gateway;

import br.com.fiap.feedback.domain.feedback.event.CriticalFeedbackEvent;

/** Publica o feedback crítico para a função de notificação. A infra decide em qual fila. */
public interface CriticalFeedbackEventGateway {

    void publish(CriticalFeedbackEvent event);
}
