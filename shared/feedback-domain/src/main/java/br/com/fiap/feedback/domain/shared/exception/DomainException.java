package br.com.fiap.feedback.domain.shared.exception;

/**
 * Base de toda violação de regra de negócio.
 *
 * <p>É abstrata porque cada regra tem a sua subclasse, e estende {@link RuntimeException}
 * para não obrigar {@code throws} em todas as camadas.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
