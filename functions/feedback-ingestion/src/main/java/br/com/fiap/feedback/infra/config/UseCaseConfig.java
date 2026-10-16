package br.com.fiap.feedback.infra.config;

import br.com.fiap.feedback.application.feedback.ReceiveFeedbackUseCase;
import br.com.fiap.feedback.application.feedback.gateway.CriticalFeedbackEventGateway;
import br.com.fiap.feedback.application.feedback.gateway.FeedbackGateway;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Clock;

/** Monta os casos de uso com CDI, para as classes da application continuarem sem anotações. */
public class UseCaseConfig {

    @Produces
    @Singleton
    @DefaultBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Produces
    @Singleton
    public ReceiveFeedbackUseCase receiveFeedbackUseCase(FeedbackGateway feedbackGateway,
            CriticalFeedbackEventGateway eventGateway, Clock clock) {
        return new ReceiveFeedbackUseCase(feedbackGateway, eventGateway, clock);
    }
}
