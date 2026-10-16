package br.com.fiap.feedback.infra.messaging;

import java.util.UUID;

/** Resposta da função notify-critical-feedback: qual feedback e se o alerta foi enviado. */
public record NotificationResult(UUID feedbackId, boolean notified) {
}
