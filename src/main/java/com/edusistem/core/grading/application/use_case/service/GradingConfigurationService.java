package com.edusistem.core.grading.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.grading.application.use_case.dtos.GradingCommands;
import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.entity.GradingWeight;
import com.edusistem.core.grading.domain.inputports.ConfigureGradingUseCase;
import com.edusistem.core.grading.domain.outputports.EvaluationResultsPort;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.grading.domain.vo.GradingConfigurationView;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingConfigurationService implements ConfigureGradingUseCase {

    private final GradingConfigurationRepositoryPort configurations;
    private final GradingScaleRepositoryPort scales;
    private final EvaluationCategoryRepositoryPort categories;
    private final EvaluationResultsPort results;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public GradingConfigurationService(GradingConfigurationRepositoryPort configurations, GradingScaleRepositoryPort scales,
                                       EvaluationCategoryRepositoryPort categories, EvaluationResultsPort results,
                                       OwnershipGuard guard, RecordAuditUseCase audit) {
        this.configurations = configurations;
        this.scales = scales;
        this.categories = categories;
        this.results = results;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    @Transactional
    public GradingConfigurationView save(GradingCommands.SaveConfiguration command) {
        guard.requireTeachingPeriod(command.teacherId(), command.teachingPeriodId());
        GradingScale scale = scales.findById(command.gradingScaleId())
                .orElseThrow(() -> ResourceNotFoundException.of("GradingScale", command.gradingScaleId()));
        List<GradingWeight> weights = new ArrayList<>();
        for (GradingCommands.WeightInput input : command.weights()) {
            categories.findById(input.evaluationCategoryId())
                    .orElseThrow(() -> ResourceNotFoundException.of("EvaluationCategory", input.evaluationCategoryId()));
            weights.add(GradingWeight.builder().evaluationCategoryId(input.evaluationCategoryId())
                    .weight(input.weight()).build());
        }
        GradingConfiguration existing = configurations.findByTeachingPeriodId(command.teachingPeriodId()).orElse(null);
        if (existing != null && !existing.getGradingScaleId().equals(scale.getId())
                && results.hasGradedExamResults(command.teachingPeriodId())) {
            throw new ConflictException("GRADING_SCALE_LOCKED",
                    "The scale cannot change because exams already have final grades in this period");
        }
        GradingConfiguration configuration = existing != null ? existing : GradingConfiguration.builder()
                .teachingPeriodId(command.teachingPeriodId()).build();
        configuration.setGradingScaleId(scale.getId());
        configuration.setWeights(weights);
        configuration.validateForSave();
        GradingConfiguration saved = configurations.save(configuration);
        audit.success(command.teacherId(), existing == null ? AuditAction.CREATE : AuditAction.UPDATE,
                "GradingConfiguration", saved.getId(), "total weight " + saved.totalWeight());
        return new GradingConfigurationView(saved, scale);
    }

    @Override
    public GradingConfigurationView get(Long teacherId, Long teachingPeriodId) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        GradingConfiguration configuration = configurations.findByTeachingPeriodId(teachingPeriodId)
                .orElseThrow(() -> new ResourceNotFoundException("GRADING_CONFIGURATION_NOT_FOUND",
                        "No grading configuration for teaching period " + teachingPeriodId));
        GradingScale scale = scales.findById(configuration.getGradingScaleId())
                .orElseThrow(() -> ResourceNotFoundException.of("GradingScale", configuration.getGradingScaleId()));
        return new GradingConfigurationView(configuration, scale);
    }
}
