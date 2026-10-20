package br.com.fiap.feedback.infra.persistence;

import br.com.fiap.feedback.application.report.gateway.FeedbackQueryGateway;
import br.com.fiap.feedback.domain.feedback.Feedback;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;

/** Não consulta nada até existir o banco: o relatório sai zerado. Sai de cena quando houver outro FeedbackQueryGateway. */
@DefaultBean
@ApplicationScoped
public class NoOpFeedbackQueryGateway implements FeedbackQueryGateway {

    @Override
    public List<Feedback> findCreatedBetween(Instant startInclusive, Instant endExclusive) {
        return List.of();
    }
}
