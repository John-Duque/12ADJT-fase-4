package br.com.fiap.feedback.domain.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Urgency")
class UrgencyTest {

    @DisplayName("Classifica a nota na urgência da sua faixa")
    @ParameterizedTest(name = "nota {0} → {1}")
    @CsvSource({
            "0, HIGH",
            "3, HIGH",
            "4, MEDIUM",
            "6, MEDIUM",
            "7, LOW",
            "10, LOW"
    })
    void shouldClassifyRatingIntoUrgency(int rating, Urgency expected) {
        assertThat(Urgency.fromRating(rating)).isEqualTo(expected);
    }

    @DisplayName("Rejeita nota fora do intervalo de 0 a 10")
    @ParameterizedTest(name = "nota {0}")
    @ValueSource(ints = {-1, 11, 100})
    void shouldRejectRatingOutOfRange(int rating) {
        assertThatThrownBy(() -> Urgency.fromRating(rating))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessageContaining("entre 0 e 10");
    }

    @DisplayName("Só a urgência HIGH é crítica")
    @ParameterizedTest(name = "{0} → crítica = {1}")
    @CsvSource({
            "HIGH, true",
            "MEDIUM, false",
            "LOW, false"
    })
    void onlyHighUrgencyIsCritical(Urgency urgency, boolean expected) {
        assertThat(urgency.isCritical()).isEqualTo(expected);
    }
}
