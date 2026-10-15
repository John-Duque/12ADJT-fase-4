package br.com.fiap.feedback.domain.feedback;

/**
 * Urgência de um feedback, derivada da nota de 0 a 10.
 *
 * <p>Notas de 0 a 3 são {@link #HIGH} (críticas), de 4 a 6 são {@link #MEDIUM} e de 7 a 10 são {@link #LOW}.
 */
public enum Urgency {
    HIGH,
    MEDIUM,
    LOW;

    public static final int MIN_RATING = 0;
    public static final int MAX_RATING = 10;

    private static final int HIGH_UPPER_BOUND = 3;
    private static final int MEDIUM_UPPER_BOUND = 6;

    /**
     * Classifica a nota em uma urgência.
     *
     * @throws InvalidFeedbackException se a nota estiver fora de 0 a 10
     */
    public static Urgency fromRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new InvalidFeedbackException(
                    "A nota deve estar entre %d e %d, recebido: %d".formatted(MIN_RATING, MAX_RATING, rating));
        }
        if (rating <= HIGH_UPPER_BOUND) {
            return HIGH;
        }
        if (rating <= MEDIUM_UPPER_BOUND) {
            return MEDIUM;
        }
        return LOW;
    }

    /** Só a urgência {@link #HIGH} gera alerta imediato aos administradores. */
    public boolean isCritical() {
        return this == HIGH;
    }
}
