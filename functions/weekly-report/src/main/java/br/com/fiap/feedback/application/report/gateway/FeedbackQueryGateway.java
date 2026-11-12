package br.com.fiap.feedback.application.report.gateway;

import br.com.fiap.feedback.domain.feedback.Feedback;
import java.time.Instant;
import java.util.List;

/** Busca os feedbacks enviados num intervalo. A infra decide de onde. */
public interface FeedbackQueryGateway {

    List<Feedback> findCreatedBetween(Instant startInclusive, Instant endExclusive);
}
