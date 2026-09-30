package com.edusistem.core.grading.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.grading.application.use_case.dtos.GradingCommands;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.inputports.ManageGradingScaleUseCase;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import java.util.List;

/** Escalas del sistema (solo lectura, para todos) más las privadas que crea cada profesor. */
public class GradingScaleService implements ManageGradingScaleUseCase {

    private final GradingScaleRepositoryPort scales;
    private final RecordAuditUseCase audit;

    public GradingScaleService(GradingScaleRepositoryPort scales, RecordAuditUseCase audit) {
        this.scales = scales;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public GradingScale create(GradingCommands.CreateScale command) {
        GradingScale scale = GradingScale.builder().teacherId(command.teacherId())
                .name(command.name() == null ? null : command.name().trim())
                .minimumValue(command.minimumValue()).maximumValue(command.maximumValue()).build();
        scale.validate();
        GradingScale saved = scales.save(scale);
        audit.success(command.teacherId(), AuditAction.CREATE, "GradingScale", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    public GradingScale get(Long teacherId, Long scaleId) {
        return scales.findById(scaleId).filter(s -> s.isVisibleTo(teacherId)).orElseThrow(() -> ResourceNotFoundException.of("GradingScale", scaleId));
    }

    @Override
    public List<GradingScale> list(Long teacherId) {
        return scales.findVisibleTo(teacherId);
    }
}
