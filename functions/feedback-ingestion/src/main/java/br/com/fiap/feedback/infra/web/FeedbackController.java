package br.com.fiap.feedback.infra.web;

import br.com.fiap.feedback.application.feedback.ReceiveFeedbackUseCase;
import br.com.fiap.feedback.infra.web.dto.FeedbackRequest;
import br.com.fiap.feedback.infra.web.dto.FeedbackResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;

/** Endpoint de entrada das avaliações dos estudantes. */
@Path("/avaliacao")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FeedbackController {

    private final ReceiveFeedbackUseCase receiveFeedbackUseCase;

    public FeedbackController(ReceiveFeedbackUseCase receiveFeedbackUseCase) {
        this.receiveFeedbackUseCase = receiveFeedbackUseCase;
    }

    /** Recebe a avaliação e responde 201 com a urgência calculada. */
    @POST
    public RestResponse<FeedbackResponse> receive(
            @Valid @NotNull(message = "O corpo da requisição é obrigatório") FeedbackRequest request) {
        var feedback = receiveFeedbackUseCase.execute(request.toInput());
        return RestResponse.status(Response.Status.CREATED, FeedbackResponse.from(feedback));
    }
}
