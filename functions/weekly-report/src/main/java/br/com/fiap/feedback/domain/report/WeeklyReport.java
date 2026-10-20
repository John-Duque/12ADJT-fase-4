package br.com.fiap.feedback.domain.report;

import br.com.fiap.feedback.domain.feedback.Feedback;
import br.com.fiap.feedback.domain.feedback.Urgency;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Relatório de um período: média das notas, quantidade por dia e por urgência e a lista das avaliações. */
public final class WeeklyReport {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final long totalFeedbacks;
    private final double averageRating;
    private final Map<LocalDate, Long> countByDay;
    private final Map<Urgency, Long> countByUrgency;
    private final List<Entry> feedbacks;

    private WeeklyReport(LocalDate startDate, LocalDate endDate, long totalFeedbacks, double averageRating,
            Map<LocalDate, Long> countByDay, Map<Urgency, Long> countByUrgency, List<Entry> feedbacks) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalFeedbacks = totalFeedbacks;
        this.averageRating = averageRating;
        this.countByDay = Collections.unmodifiableMap(countByDay);
        this.countByUrgency = Collections.unmodifiableMap(countByUrgency);
        this.feedbacks = feedbacks;
    }

    /** Monta o relatório de startDate a endDate (inclusive), contando cada feedback no dia do fuso informado. */
    public static WeeklyReport of(LocalDate startDate, LocalDate endDate, List<Feedback> feedbacks, ZoneId zone) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate é obrigatório");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("endDate é obrigatório");
        }
        if (feedbacks == null) {
            throw new IllegalArgumentException("feedbacks é obrigatório");
        }
        if (zone == null) {
            throw new IllegalArgumentException("zone é obrigatório");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate deve ser igual ou posterior a startDate");
        }
        return new WeeklyReport(startDate, endDate, feedbacks.size(), averageRating(feedbacks),
                countByDay(startDate, endDate, feedbacks, zone), countByUrgency(feedbacks), entries(feedbacks));
    }

    private static Map<LocalDate, Long> countByDay(LocalDate startDate, LocalDate endDate, List<Feedback> feedbacks,
            ZoneId zone) {
        var countByDay = new TreeMap<LocalDate, Long>();
        for (var day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            countByDay.put(day, 0L);
        }
        for (var feedback : feedbacks) {
            countByDay.merge(LocalDate.ofInstant(feedback.getCreatedAt(), zone), 1L, Long::sum);
        }
        return countByDay;
    }

    private static Map<Urgency, Long> countByUrgency(List<Feedback> feedbacks) {
        var countByUrgency = new EnumMap<Urgency, Long>(Urgency.class);
        for (var urgency : Urgency.values()) {
            countByUrgency.put(urgency, 0L);
        }
        for (var feedback : feedbacks) {
            countByUrgency.merge(feedback.getUrgency(), 1L, Long::sum);
        }
        return countByUrgency;
    }

    private static double averageRating(List<Feedback> feedbacks) {
        if (feedbacks.isEmpty()) {
            return 0.0;
        }
        long sum = feedbacks.stream().mapToLong(Feedback::getRating).sum();
        return BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(feedbacks.size()), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static List<Entry> entries(List<Feedback> feedbacks) {
        return feedbacks.stream()
                .sorted(Comparator.comparing(Feedback::getCreatedAt))
                .map(feedback -> new Entry(feedback.getDescription(), feedback.getUrgency(), feedback.getCreatedAt()))
                .toList();
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public long getTotalFeedbacks() {
        return totalFeedbacks;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public Map<LocalDate, Long> getCountByDay() {
        return countByDay;
    }

    public Map<Urgency, Long> getCountByUrgency() {
        return countByUrgency;
    }

    public List<Entry> getFeedbacks() {
        return feedbacks;
    }

    /** Linha do relatório: os dados de cada avaliação exigidos pelo enunciado. */
    public static final class Entry {

        private final String description;
        private final Urgency urgency;
        private final Instant createdAt;

        public Entry(String description, Urgency urgency, Instant createdAt) {
            this.description = description;
            this.urgency = urgency;
            this.createdAt = createdAt;
        }

        public String getDescription() {
            return description;
        }

        public Urgency getUrgency() {
            return urgency;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }
    }
}
