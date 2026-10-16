package br.com.fiap.feedback.infra.shared.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Corpo das respostas de erro da API. */
public record ErrorResponse(@JsonProperty("mensagem") String message) {
}
