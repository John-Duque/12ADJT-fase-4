package br.com.fiap.feedback.infra.shared.exception;

import br.com.fiap.feedback.domain.shared.exception.DomainException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Traduz exceções para HTTP em toda a API. Erros de Bean Validation o Quarkus já converte em 400. */
public class GlobalExceptionHandler {

    /** Regra de negócio violada vira 400 com a mensagem do domínio. */
    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleDomainException(DomainException exception) {
        return RestResponse.status(Response.Status.BAD_REQUEST, new ErrorResponse(exception.getMessage()));
    }
}
