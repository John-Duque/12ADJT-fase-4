package br.com.fiap.feedback.application.feedback;

/** Dados recebidos para criar um feedback. Não valida nada: quem valida é o domínio. */
public final class ReceiveFeedbackInput {

    private final String description;
    private final Integer rating;

    public ReceiveFeedbackInput(String description, Integer rating) {
        this.description = description;
        this.rating = rating;
    }

    public String getDescription() {
        return description;
    }

    public Integer getRating() {
        return rating;
    }
}
