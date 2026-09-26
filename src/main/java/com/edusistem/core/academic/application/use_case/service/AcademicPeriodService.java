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
import com.edusistem.core.shared.domain.outputports.CatalogUsagePort;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public class AcademicPeriodService implements ManageAcademicPeriodUseCase {

    private final AcademicPeriodRepositoryPort periods;
    private final CatalogUsagePort usage;
    private final RecordAuditUseCase audit;

    public AcademicPeriodService(AcademicPeriodRepositoryPort periods, CatalogUsagePort usage, RecordAuditUseCase audit) {
        this.periods = periods;
        this.usage = usage;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public AcademicPeriod create(AcademicCommands.SavePeriod command) {
        AcademicPeriod period = AcademicPeriod.builder().name(command.name().trim())
                .startDate(command.startDate()).endDate(command.endDate()).build();
        period.validateDates();
        AcademicPeriod saved = periods.save(period);
        audit.success(command.actorId(), AuditAction.CREATE, "AcademicPeriod", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    @UseCaseTransactional
    public AcademicPeriod update(AcademicCommands.SavePeriod command) {
        AcademicPeriod period = get(command.periodId());
        if (usage.academicPeriodUsedByOtherTeachers(period.getId(), command.actorId())) {
            throw new ConflictException("CATALOG_ITEM_IN_USE",
                    "The academic period is used by other teachers and cannot be modified");
        }
        period.setName(command.name().trim());
        period.setStartDate(command.startDate());
        period.setEndDate(command.endDate());
        period.validateDates();
        AcademicPeriod saved = periods.save(period);
        audit.success(command.actorId(), AuditAction.UPDATE, "AcademicPeriod", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    @UseCaseTransactional
    public void delete(Long actorId, Long periodId) {
        AcademicPeriod period = get(periodId);
        if (periods.hasTeachingPeriods(periodId)) {
            throw new ConflictException("ACADEMIC_PERIOD_IN_USE", "The academic period is in use and cannot be deleted");
        }
        periods.deleteById(periodId);
        audit.success(actorId, AuditAction.DELETE, "AcademicPeriod", periodId, period.getName());
    }

    @Override
    public AcademicPeriod get(Long periodId) {
        return periods.findById(periodId).orElseThrow(() -> ResourceNotFoundException.of("AcademicPeriod", periodId));
    }

    @Override
    public PageResult<AcademicPeriod> list(PageQuery page) {
        return periods.findAll(page);
    }
}
