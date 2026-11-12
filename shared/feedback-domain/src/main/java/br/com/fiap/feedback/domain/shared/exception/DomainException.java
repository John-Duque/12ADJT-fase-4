package br.com.fiap.feedback.domain.shared.exception;

/** Base das exceções de regra de negócio. Cada regra tem a sua subclasse. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
