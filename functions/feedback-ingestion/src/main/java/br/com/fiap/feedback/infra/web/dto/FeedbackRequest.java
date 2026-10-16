package br.com.fiap.feedback.infra.web.dto;

import br.com.fiap.feedback.application.feedback.ReceiveFeedbackInput;
import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.Urgency;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo do POST /avaliacao, com os nomes de campo do enunciado. */
public record FeedbackRequest(
        @JsonProperty("descricao")
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = Feedback.MAX_DESCRIPTION_LENGTH, message = "A descrição deve ter no máximo {max} caracteres")
        String description,

        @JsonProperty("nota")
        @NotNull(message = "A nota é obrigatória")
        @Min(value = Urgency.MIN_RATING, message = "A nota deve ser no mínimo {value}")
        @Max(value = Urgency.MAX_RATING, message = "A nota deve ser no máximo {value}")
        Integer rating) {

    public ReceiveFeedbackInput toInput() {
        return new ReceiveFeedbackInput(description, rating);
    }
}
