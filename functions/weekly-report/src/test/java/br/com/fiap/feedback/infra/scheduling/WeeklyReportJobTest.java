package br.com.fiap.feedback.infra.scheduling;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisplayName("WeeklyReportJob")
class WeeklyReportJobTest {

    @Test
    @DisplayName("Gera o relatório pela função generate-weekly-report")
    void shouldGenerateReport() {
        given()
                .when().post("/generate-weekly-report")
                .then()
                .statusCode(200)
                .body("startDate", notNullValue())
                .body("endDate", notNullValue())
                .body("totalFeedbacks", equalTo(0));
    }
}
