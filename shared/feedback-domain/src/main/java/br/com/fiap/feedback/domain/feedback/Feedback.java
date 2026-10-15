package br.com.fiap.feedback.domain.feedback;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Avaliação de uma aula. O construtor valida todas as regras, então não existe Feedback inválido. */
public final class Feedback {

    public static final int MAX_DESCRIPTION_LENGTH = 1000;

    private final UUID id;
    private final String description;
    private final int rating;
    private final Urgency urgency;
    private final Instant createdAt;

    public Feedback(UUID id, String description, int rating, Urgency urgency, Instant createdAt) {
        if (id == null) {
            throw new IllegalArgumentException("id é obrigatório");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt é obrigatório");
        }
        this.id = id;
        this.createdAt = createdAt;
        this.description = validateDescription(description);
        this.rating = rating;
        this.urgency = validateUrgency(urgency, rating);
    }

    /** Cria um novo feedback: gera o id, calcula a urgência pela nota e usa o relógio como data de envio. */
    public static Feedback create(String description, int rating, Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock é obrigatório");
        }
        return new Feedback(UUID.randomUUID(), description, rating, Urgency.fromRating(rating), clock.instant());
    }

    private static String validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new InvalidFeedbackException("A descrição é obrigatória");
        }
        var stripped = description.strip();
        if (stripped.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidFeedbackException(
                    "A descrição deve ter no máximo %d caracteres".formatted(MAX_DESCRIPTION_LENGTH));
        }
        return stripped;
    }

    private static Urgency validateUrgency(Urgency urgency, int rating) {
        var expectedUrgency = Urgency.fromRating(rating);
        if (urgency != expectedUrgency) {
            throw new IllegalArgumentException("Urgência %s não corresponde à nota %d (esperado %s)"
                    .formatted(urgency, rating, expectedUrgency));
        }
        return urgency;
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

    /** Feedback crítico gera alerta imediato aos administradores. */
    public boolean isCritical() {
        return urgency.isCritical();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Feedback feedback && id.equals(feedback.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Feedback[id=%s, rating=%d, urgency=%s, createdAt=%s, description=%s]"
                .formatted(id, rating, urgency, createdAt, description);
    }
}
