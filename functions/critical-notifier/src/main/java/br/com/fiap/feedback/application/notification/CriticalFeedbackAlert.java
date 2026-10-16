package br.com.fiap.feedback.application.notification;

import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.Urgency;
import java.time.Instant;
import java.util.Objects;

/** Conteúdo do e-mail de urgência: descrição, urgência e data de envio, como pede o enunciado. */
public final class CriticalFeedbackAlert {

    private final String description;
    private final Urgency urgency;
    private final Instant sentAt;

    public CriticalFeedbackAlert(String description, Urgency urgency, Instant sentAt) {
        this.description = description;
        this.urgency = urgency;
        this.sentAt = sentAt;
    }

    public static CriticalFeedbackAlert from(Feedback feedback) {
        if (feedback == null) {
            throw new IllegalArgumentException("feedback é obrigatório");
        }
        return new CriticalFeedbackAlert(feedback.getDescription(), feedback.getUrgency(), feedback.getCreatedAt());
    }

    public String getDescription() {
        return description;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof CriticalFeedbackAlert alert
                && Objects.equals(description, alert.description)
                && urgency == alert.urgency
                && Objects.equals(sentAt, alert.sentAt));
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, urgency, sentAt);
    }

    @Override
    public String toString() {
        return "CriticalFeedbackAlert[urgency=%s, sentAt=%s, description=%s]"
                .formatted(urgency, sentAt, description);
    }
}
