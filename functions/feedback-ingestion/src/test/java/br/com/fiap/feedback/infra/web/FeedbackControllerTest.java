package br.com.fiap.feedback.infra.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisplayName("FeedbackController")
class FeedbackControllerTest {

    private static final String PATH = "/avaliacao";

    @Test
    @DisplayName("Cria a avaliação e responde 201 com urgência LOW para nota alta")
    void shouldCreateFeedback() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"descricao":"Aula muito clara","nota":9}
                        """)
                .when().post(PATH)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("dataEnvio", notNullValue())
                .body("urgencia", equalTo("LOW"));
    }

    @Test
    @DisplayName("Classifica nota baixa como urgência HIGH")
    void shouldClassifyLowRatingAsHighUrgency() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"descricao":"Não consegui assistir","nota":2}
                        """)
                .when().post(PATH)
                .then()
                .statusCode(201)
                .body("urgencia", equalTo("HIGH"));
    }

    @Test
    @DisplayName("Rejeita nota fora de 0 a 10 com 400")
    void shouldRejectRatingOutOfRange() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"descricao":"x","nota":11}
                        """)
                .when().post(PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Rejeita avaliação sem descrição com 400")
    void shouldRejectMissingDescription() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nota":5}
                        """)
                .when().post(PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Rejeita avaliação sem nota com 400")
    void shouldRejectMissingRating() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"descricao":"x"}
                        """)
                .when().post(PATH)
                .then()
                .statusCode(400);
    }
}
