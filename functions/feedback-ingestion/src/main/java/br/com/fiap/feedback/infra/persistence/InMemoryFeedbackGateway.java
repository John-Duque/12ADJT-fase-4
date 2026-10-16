package br.com.fiap.feedback.infra.persistence;

import br.com.fiap.feedback.application.feedback.gateway.FeedbackGateway;
import br.com.fiap.feedback.domain.feedback.Feedback;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Guarda os feedbacks em memória até existir o banco. Sai de cena quando houver outro FeedbackGateway. */
@DefaultBean
@ApplicationScoped
public class InMemoryFeedbackGateway implements FeedbackGateway {

    private final Map<UUID, Feedback> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Feedback feedback) {
        storage.put(feedback.getId(), feedback);
    }

    public List<Feedback> findAll() {
        return List.copyOf(storage.values());
    }
}
