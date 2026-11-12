package br.com.fiap.feedback.infra.messaging;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisplayName("CriticalFeedbackConsumer")
class CriticalFeedbackConsumerTest {

    private static final String PATH = "/notify-critical-feedback";
    private static final String FEEDBACK_ID = "8f14e45f-ceea-467a-9b8e-6c2a9b8f1a11";

    @Test
    @DisplayName("Notifica o evento crítico")
    void shouldNotifyCriticalEvent() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"id":"%s","description":"Aula sem áudio","rating":1,"urgency":"HIGH","createdAt":"2026-10-13T12:00:00Z"}
                        """.formatted(FEEDBACK_ID))
                .when().post(PATH)
                .then()
                .statusCode(200)
                .body("feedbackId", equalTo(FEEDBACK_ID))
                .body("notified", equalTo(true));
    }

    @Test
    @DisplayName("Ignora o evento não crítico")
    void shouldIgnoreNonCriticalEvent() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"id":"%s","description":"Aula boa","rating":8,"urgency":"LOW","createdAt":"2026-10-13T12:00:00Z"}
                        """.formatted(FEEDBACK_ID))
                .when().post(PATH)
                .then()
                .statusCode(200)
                .body("feedbackId", equalTo(FEEDBACK_ID))
                .body("notified", equalTo(false));
    }
}
