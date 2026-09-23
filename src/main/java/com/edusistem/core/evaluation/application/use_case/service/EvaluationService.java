package com.edusistem.core.evaluation.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.application.use_case.dtos.EvaluationCommands;
import com.edusistem.core.evaluation.domain.entity.Evaluation;
import com.edusistem.core.evaluation.domain.entity.EvaluationCategory;
import com.edusistem.core.evaluation.domain.inputports.CreateEvaluationUseCase;
import com.edusistem.core.evaluation.domain.inputports.QueryEvaluationUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.evaluation.domain.vo.EvaluationView;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationService implements CreateEvaluationUseCase, QueryEvaluationUseCase {

    private final EvaluationRepositoryPort evaluations;
    private final EvaluationCategoryRepositoryPort categories;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public EvaluationService(EvaluationRepositoryPort evaluations, EvaluationCategoryRepositoryPort categories,
                             OwnershipGuard guard, RecordAuditUseCase audit) {
        this.evaluations = evaluations;
        this.categories = categories;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    @Transactional
    public Evaluation create(EvaluationCommands.Create command) {
        guard.requireTeachingPeriod(command.teacherId(), command.teachingPeriodId());
        EvaluationCategory category = categories.findByName(command.category().name())
                .orElseThrow(() -> new BusinessRuleException("CATEGORY_NOT_CONFIGURED",
                        "Evaluation category " + command.category() + " is not configured"));
        Evaluation evaluation = Evaluation.builder()
                .teachingPeriodId(command.teachingPeriodId())
                .evaluationCategoryId(category.getId())
                .name(command.name() == null ? null : command.name().trim())
                .description(command.description() == null || command.description().isBlank() ? null : command.description().trim())
                .evaluationDate(command.evaluationDate())
                .maximumScore(command.maximumScore())
                .build();
        evaluation.validate();
        return evaluations.save(evaluation);
    }

    @Override
    public PageResult<EvaluationView> search(Long teacherId, Long teachingPeriodId, Long categoryId, PageQuery page) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        return evaluations.findViewsByTeachingPeriodId(teachingPeriodId, categoryId, page);
    }

    @Override
    public EvaluationView get(Long teacherId, Long evaluationId) {
        guard.requireEvaluation(teacherId, evaluationId);
        return evaluations.findViewById(evaluationId).orElseThrow(() -> ResourceNotFoundException.of("Evaluation", evaluationId));
    }

    @Override
    @Transactional
    public EvaluationView updateDetails(EvaluationCommands.UpdateDetails command) {
        guard.requireEvaluation(command.teacherId(), command.evaluationId());
        Evaluation evaluation = evaluations.findById(command.evaluationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Evaluation", command.evaluationId()));
        evaluation.updateDetails(command.name(), command.description(), command.evaluationDate());
        evaluation.validate();
        evaluations.save(evaluation);
        audit.success(command.teacherId(), AuditAction.UPDATE, "Evaluation", evaluation.getId(), evaluation.getName());
        return get(command.teacherId(), evaluation.getId());
    }

    @Override
    public List<EvaluationCategory> listCategories() {
        return categories.findAll();
    }
}
