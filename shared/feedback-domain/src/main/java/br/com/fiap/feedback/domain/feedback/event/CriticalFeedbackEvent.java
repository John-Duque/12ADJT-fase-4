package br.com.fiap.feedback.domain.feedback.event;

import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.Urgency;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Mensagem da fila de feedbacks críticos. Só transporta os dados: a validação acontece no toFeedback(). */
public final class CriticalFeedbackEvent {

    private final UUID id;
    private final String description;
    private final int rating;
    private final Urgency urgency;
    private final Instant createdAt;

    public CriticalFeedbackEvent(UUID id, String description, int rating, Urgency urgency, Instant createdAt) {
        this.id = id;
        this.description = description;
        this.rating = rating;
        this.urgency = urgency;
        this.createdAt = createdAt;
    }

    public static CriticalFeedbackEvent from(Feedback feedback) {
        if (feedback == null) {
            throw new IllegalArgumentException("feedback é obrigatório");
        }
        return new CriticalFeedbackEvent(feedback.getId(), feedback.getDescription(), feedback.getRating(),
                feedback.getUrgency(), feedback.getCreatedAt());
    }

    /** Reconstrói o Feedback, reaplicando as validações. */
    public Feedback toFeedback() {
        return new Feedback(id, description, rating, urgency, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public int getRating() {
        return rating;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof CriticalFeedbackEvent event
                && rating == event.rating
                && Objects.equals(id, event.id)
                && Objects.equals(description, event.description)
                && urgency == event.urgency
                && Objects.equals(createdAt, event.createdAt));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, rating, urgency, createdAt);
    }

    @Override
    public String toString() {
        return "CriticalFeedbackEvent[id=%s, rating=%d, urgency=%s, createdAt=%s, description=%s]"
                .formatted(id, rating, urgency, createdAt, description);
    }
}
