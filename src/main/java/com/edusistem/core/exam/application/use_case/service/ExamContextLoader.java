package com.edusistem.core.exam.application.use_case.service;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.evaluation.domain.entity.Evaluation;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.exam.domain.entity.Exam;
import com.edusistem.core.exam.domain.outputports.ExamRepositoryPort;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;

/** Carga un examen verificando ownership y reúne su contexto académico (evaluación y teaching period). */
public class ExamContextLoader {

    record ExamContext(Exam exam, Evaluation evaluation, TeachingPeriodView period) {
    }

    private final ExamRepositoryPort exams;
    private final EvaluationRepositoryPort evaluations;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final GradingConfigurationRepositoryPort configurations;
    private final GradingScaleRepositoryPort scales;
    private final OwnershipGuard guard;

    public ExamContextLoader(ExamRepositoryPort exams, EvaluationRepositoryPort evaluations,
                      TeachingPeriodRepositoryPort teachingPeriods, GradingConfigurationRepositoryPort configurations,
                      GradingScaleRepositoryPort scales, OwnershipGuard guard) {
        this.exams = exams;
        this.evaluations = evaluations;
        this.teachingPeriods = teachingPeriods;
        this.configurations = configurations;
        this.scales = scales;
        this.guard = guard;
    }

    ExamContext load(Long teacherId, Long examId) {
        guard.requireExam(teacherId, examId);
        Exam exam = exams.findById(examId).orElseThrow(() -> ResourceNotFoundException.of("Exam", examId));
        Evaluation evaluation = evaluations.findById(exam.getEvaluationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Evaluation", exam.getEvaluationId()));
        TeachingPeriodView period = teachingPeriods.findViewById(evaluation.getTeachingPeriodId())
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", evaluation.getTeachingPeriodId()));
        return new ExamContext(exam, evaluation, period);
    }

    GradingScale scaleOrNull(Long teachingPeriodId) {
        return configurations.findByTeachingPeriodId(teachingPeriodId)
                .flatMap(c -> scales.findById(c.getGradingScaleId())).orElse(null);
    }

    /** La escala del periodo; sin configuración no se puede convertir la puntuación a nota final. */
    GradingScale requireScale(Long teachingPeriodId) {
        GradingScale scale = scaleOrNull(teachingPeriodId);
        if (scale == null) {
            throw new BusinessRuleException("GRADING_CONFIGURATION_REQUIRED",
                    "Configure the grading scale of the teaching period before grading exams");
        }
        return scale;
    }
}
