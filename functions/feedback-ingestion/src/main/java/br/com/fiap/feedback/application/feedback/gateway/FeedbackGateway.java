package br.com.fiap.feedback.application.feedback.gateway;

import br.com.fiap.feedback.domain.feedback.Feedback;

/** Grava o feedback recebido. A infra decide onde. */
public interface FeedbackGateway {

    void save(Feedback feedback);
}
