package com.edusistem.core.academic.application.use_case.service;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.academic.domain.inputports.ManageAcademicPeriodUseCase;
import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

/** Periodos académicos propios del profesor; los de otros profesores responden 404. */
public class AcademicPeriodService implements ManageAcademicPeriodUseCase {

    private final AcademicPeriodRepositoryPort periods;
    private final RecordAuditUseCase audit;

    public AcademicPeriodService(AcademicPeriodRepositoryPort periods, RecordAuditUseCase audit) {
        this.periods = periods;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public AcademicPeriod create(AcademicCommands.SavePeriod command) {
        AcademicPeriod period = AcademicPeriod.builder().teacherId(command.teacherId()).name(command.name().trim())
                .startDate(command.startDate()).endDate(command.endDate()).build();
        period.validateDates();
        AcademicPeriod saved = periods.save(period);
        audit.success(command.teacherId(), AuditAction.CREATE, "AcademicPeriod", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    @UseCaseTransactional
    public AcademicPeriod update(AcademicCommands.SavePeriod command) {
        AcademicPeriod period = get(command.teacherId(), command.periodId());
        period.setName(command.name().trim());
        period.setStartDate(command.startDate());
        period.setEndDate(command.endDate());
        period.validateDates();
        AcademicPeriod saved = periods.save(period);
        audit.success(command.teacherId(), AuditAction.UPDATE, "AcademicPeriod", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    @UseCaseTransactional
    public void delete(Long teacherId, Long periodId) {
        AcademicPeriod period = get(teacherId, periodId);
        if (periods.hasTeachingPeriods(periodId)) {
            throw new ConflictException("ACADEMIC_PERIOD_IN_USE", "The academic period is in use and cannot be deleted");
        }
        periods.deleteById(periodId);
        audit.success(teacherId, AuditAction.DELETE, "AcademicPeriod", periodId, period.getName());
    }

    @Override
    public AcademicPeriod get(Long teacherId, Long periodId) {
        return periods.findById(periodId).filter(p -> p.getTeacherId().equals(teacherId))
                .orElseThrow(() -> ResourceNotFoundException.of("AcademicPeriod", periodId));
    }

    @Override
    public PageResult<AcademicPeriod> list(Long teacherId, PageQuery page) {
        return periods.findByTeacherId(teacherId, page);
    }
}
