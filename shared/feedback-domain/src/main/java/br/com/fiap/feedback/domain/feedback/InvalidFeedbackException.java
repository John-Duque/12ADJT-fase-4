package br.com.fiap.feedback.domain.feedback;

import br.com.fiap.feedback.domain.shared.exception.DomainException;

/**
 * Violação de regra do feedback, como nota fora do intervalo ou descrição inválida.
 */
public class InvalidFeedbackException extends DomainException {

    public InvalidFeedbackException(String message) {
        super(message);
    }
}
