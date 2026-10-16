package br.com.fiap.feedback.application.feedback;

import br.com.fiap.feedback.application.feedback.gateway.CriticalFeedbackEventGateway;
import br.com.fiap.feedback.application.feedback.gateway.FeedbackGateway;
import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.InvalidFeedbackException;
import br.com.fiap.feedback.domain.feedback.event.CriticalFeedbackEvent;
import java.time.Clock;

/** Recebe a avaliação, grava e, se for crítica, publica para a notificação. */
public class ReceiveFeedbackUseCase {

    private final FeedbackGateway feedbackGateway;
    private final CriticalFeedbackEventGateway eventGateway;
    private final Clock clock;

    public ReceiveFeedbackUseCase(FeedbackGateway feedbackGateway, CriticalFeedbackEventGateway eventGateway,
            Clock clock) {
        if (feedbackGateway == null) {
            throw new IllegalArgumentException("feedbackGateway é obrigatório");
        }
        if (eventGateway == null) {
            throw new IllegalArgumentException("eventGateway é obrigatório");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock é obrigatório");
        }
        this.feedbackGateway = feedbackGateway;
        this.eventGateway = eventGateway;
        this.clock = clock;
    }

    public Feedback execute(ReceiveFeedbackInput input) {
        if (input == null || input.getRating() == null) {
            throw new InvalidFeedbackException("A nota é obrigatória");
        }
        var feedback = Feedback.create(input.getDescription(), input.getRating(), clock);
        feedbackGateway.save(feedback);
        if (feedback.isCritical()) {
            eventGateway.publish(CriticalFeedbackEvent.from(feedback));
        }
        return feedback;
    }
}
