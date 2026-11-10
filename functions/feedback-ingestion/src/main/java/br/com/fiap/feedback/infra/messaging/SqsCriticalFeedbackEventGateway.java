package br.com.fiap.feedback.infra.messaging;

import br.com.fiap.feedback.application.feedback.gateway.CriticalFeedbackEventGateway;
import br.com.fiap.feedback.domain.feedback.event.CriticalFeedbackEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/** Publica o evento crítico na fila do SQS. Substitui o LoggingCriticalFeedbackEventGateway no build do profile lambda. */
@ApplicationScoped
@IfBuildProperty(name = "feedback.aws.enabled", stringValue = "true")
public class SqsCriticalFeedbackEventGateway implements CriticalFeedbackEventGateway {

    private static final Logger LOG = Logger.getLogger(SqsCriticalFeedbackEventGateway.class);

    private final SqsClient client;
    private final ObjectMapper mapper;
    private final String queueUrl;

    public SqsCriticalFeedbackEventGateway(SqsClient client, ObjectMapper mapper,
            @ConfigProperty(name = "critical.queue.url") String queueUrl) {
        this.client = client;
        this.mapper = mapper;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publish(CriticalFeedbackEvent event) {
        try {
            client.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(toJson(event))
                    .build());
        } catch (SdkException e) {
            LOG.errorf(e, "Falha ao publicar feedback crítico na fila: id=%s", event.getId());
            throw e;
        }
        LOG.infof("Feedback crítico publicado na fila: id=%s urgencia=%s", event.getId(), event.getUrgency());
    }

    private String toJson(CriticalFeedbackEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível converter o evento crítico em JSON", e);
        }
    }
}
