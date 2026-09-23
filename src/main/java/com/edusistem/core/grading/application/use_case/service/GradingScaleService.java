package com.edusistem.core.grading.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.grading.application.use_case.dtos.GradingCommands;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.inputports.ManageGradingScaleUseCase;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingScaleService implements ManageGradingScaleUseCase {

    private final GradingScaleRepositoryPort scales;
    private final RecordAuditUseCase audit;

    public GradingScaleService(GradingScaleRepositoryPort scales, RecordAuditUseCase audit) {
        this.scales = scales;
        this.audit = audit;
    }

    @Override
    @Transactional
    public GradingScale create(GradingCommands.CreateScale command) {
        GradingScale scale = GradingScale.builder().name(command.name() == null ? null : command.name().trim())
                .minimumValue(command.minimumValue()).maximumValue(command.maximumValue()).build();
        scale.validate();
        GradingScale saved = scales.save(scale);
        audit.success(command.actorId(), AuditAction.CREATE, "GradingScale", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    public GradingScale get(Long scaleId) {
        return scales.findById(scaleId).orElseThrow(() -> ResourceNotFoundException.of("GradingScale", scaleId));
    }

    @Override
    public List<GradingScale> list() {
        return scales.findAll();
    }
}
