package com.edusistem.core.exam.infrastructure.config;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.domain.inputports.CreateEvaluationUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.exam.application.use_case.service.AnswerSheetService;
import com.edusistem.core.exam.application.use_case.service.ExamContextLoader;
import com.edusistem.core.exam.application.use_case.service.ExamService;
import com.edusistem.core.exam.application.use_case.service.SubmissionDetailsAssembler;
import com.edusistem.core.exam.application.use_case.service.SubmissionProcessingService;
import com.edusistem.core.exam.application.use_case.service.SubmissionReviewService;
import com.edusistem.core.exam.domain.outputports.AnswerSheetProcessorPort;
import com.edusistem.core.exam.domain.outputports.AnswerSheetRendererPort;
import com.edusistem.core.exam.domain.outputports.ExamRepositoryPort;
import com.edusistem.core.exam.domain.outputports.ExamSubmissionRepositoryPort;
import com.edusistem.core.exam.domain.service.BubbleClassifier;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo exam. */
@Configuration
public class ExamBeanConfig {

    @Bean
    ExamContextLoader examContextLoader(ExamRepositoryPort exams, EvaluationRepositoryPort evaluations,
                                        TeachingPeriodRepositoryPort teachingPeriods,
                                        GradingConfigurationRepositoryPort configurations,
                                        GradingScaleRepositoryPort scales, OwnershipGuard guard) {
        return new ExamContextLoader(exams, evaluations, teachingPeriods, configurations, scales, guard);
    }

    @Bean
    SubmissionDetailsAssembler submissionDetailsAssembler(ExamRepositoryPort exams,
                                                          EvaluationRepositoryPort evaluations,
                                                          StudentRepositoryPort students, ExamContextLoader loader) {
        return new SubmissionDetailsAssembler(exams, evaluations, students, loader);
    }

    @Bean
    AnswerSheetService answerSheetService(ExamContextLoader loader, StudentRepositoryPort students,
                                          StudentGroupRepositoryPort studentGroups, AnswerSheetRendererPort renderer,
                                          RecordAuditUseCase audit) {
        return new AnswerSheetService(loader, students, studentGroups, renderer, audit);
    }

    @Bean
    ExamService examService(ExamRepositoryPort exams, EvaluationRepositoryPort evaluations,
                            CreateEvaluationUseCase createEvaluation, ExamContextLoader loader, OwnershipGuard guard,
                            RecordAuditUseCase audit) {
        return new ExamService(exams, evaluations, createEvaluation, loader, guard, audit);
    }

    @Bean
    SubmissionProcessingService submissionProcessingService(ExamContextLoader loader,
                                                            AnswerSheetProcessorPort processor,
                                                            ExamSubmissionRepositoryPort submissions,
                                                            StudentRepositoryPort students,
                                                            StudentGroupRepositoryPort studentGroups,
                                                            FileStoragePort storage, BubbleClassifier classifier,
                                                            SubmissionDetailsAssembler assembler,
                                                            RecordAuditUseCase audit, Clock clock) {
        return new SubmissionProcessingService(loader, processor, submissions, students, studentGroups, storage,
                                               classifier, assembler, audit, clock);
    }

    @Bean
    SubmissionReviewService submissionReviewService(ExamContextLoader loader,
                                                    ExamSubmissionRepositoryPort submissions,
                                                    SubmissionDetailsAssembler assembler, OwnershipGuard guard,
                                                    RecordAuditUseCase audit, FileStoragePort storage, Clock clock) {
        return new SubmissionReviewService(loader, submissions, assembler, guard, audit, storage, clock);
    }
}
