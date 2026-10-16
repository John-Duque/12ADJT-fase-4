package br.com.fiap.feedback.infra.web.dto;

import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.Urgency;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/** Resposta do POST /avaliacao: id, urgência calculada e data de envio. */
public record FeedbackResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("urgencia") Urgency urgency,
        @JsonProperty("dataEnvio") Instant createdAt) {

    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(feedback.getId(), feedback.getUrgency(), feedback.getCreatedAt());
    }
}
