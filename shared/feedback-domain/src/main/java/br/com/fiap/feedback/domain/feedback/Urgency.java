package br.com.fiap.feedback.domain.feedback;

/** Urgência do feedback pela nota: 0 a 3 HIGH, 4 a 6 MEDIUM, 7 a 10 LOW. */
public enum Urgency {
    HIGH,
    MEDIUM,
    LOW;

    public static final int MIN_RATING = 0;
    public static final int MAX_RATING = 10;

    private static final int HIGH_UPPER_BOUND = 3;
    private static final int MEDIUM_UPPER_BOUND = 6;

    /** Classifica a nota em uma urgência. Lança InvalidFeedbackException se a nota estiver fora de 0 a 10. */
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

    /** Só HIGH é crítica. */
    public boolean isCritical() {
        return this == HIGH;
    }
}
