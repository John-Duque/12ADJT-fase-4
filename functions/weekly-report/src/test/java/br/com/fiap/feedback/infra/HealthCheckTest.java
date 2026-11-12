package br.com.fiap.feedback.infra;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisplayName("Health check")
class HealthCheckTest {

    @Test
    @DisplayName("Readiness responde 200")
    void shouldExposeReadiness() {
        given()
                .when().get("/q/health/ready")
                .then().statusCode(200);
    }
}
