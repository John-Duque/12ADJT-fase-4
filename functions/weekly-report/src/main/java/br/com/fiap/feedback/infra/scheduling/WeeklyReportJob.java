package br.com.fiap.feedback.infra.scheduling;

import br.com.fiap.feedback.application.report.GenerateWeeklyReportUseCase;
import br.com.fiap.feedback.domain.report.WeeklyReport;
import io.quarkus.funqy.Funq;
import jakarta.inject.Inject;

/** Função acionada pelo agendamento semanal: gera e entrega o relatório. Localmente, POST /generate-weekly-report. */
public class WeeklyReportJob {

    @Inject
    GenerateWeeklyReportUseCase generateWeeklyReportUseCase;

    @Funq("generate-weekly-report")
    public WeeklyReport run() {
        return generateWeeklyReportUseCase.execute();
    }
}
