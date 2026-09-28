package com.edusistem.core.academic.application.use_case.service;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.academic.domain.entity.TeachingPeriod;
import com.edusistem.core.academic.domain.entity.TeachingPeriodSchedule;
import com.edusistem.core.academic.domain.inputports.ManageTeachingPeriodScheduleUseCase;
import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodScheduleRepositoryPort;
import com.edusistem.core.academic.domain.vo.ScheduledClassView;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import java.util.Comparator;
import java.util.List;

public class TeachingPeriodScheduleService implements ManageTeachingPeriodScheduleUseCase {

    static final Comparator<ScheduledClassView> BY_DAY_AND_TIME = Comparator
            .comparing(ScheduledClassView::dayOfWeek)
            .thenComparing(ScheduledClassView::startTime)
            .thenComparing(ScheduledClassView::scheduleId);

    private final TeachingPeriodScheduleRepositoryPort schedules;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final AcademicPeriodRepositoryPort academicPeriods;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public TeachingPeriodScheduleService(TeachingPeriodScheduleRepositoryPort schedules,
                                         TeachingPeriodRepositoryPort teachingPeriods,
                                         AcademicPeriodRepositoryPort academicPeriods, OwnershipGuard guard,
                                         RecordAuditUseCase audit) {
        this.schedules = schedules;
        this.teachingPeriods = teachingPeriods;
        this.academicPeriods = academicPeriods;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    public List<ScheduledClassView> list(Long teacherId, Long teachingPeriodId) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        return schedules.findViewsByTeachingPeriodId(teachingPeriodId).stream().sorted(BY_DAY_AND_TIME).toList();
    }

    @Override
    @UseCaseTransactional
    public ScheduledClassView create(AcademicCommands.SaveSchedule command) {
        guard.requireTeachingPeriod(command.teacherId(), command.teachingPeriodId());
        TeachingPeriodSchedule schedule = TeachingPeriodSchedule.builder().teachingPeriodId(command.teachingPeriodId())
                .build();
        TeachingPeriodSchedule saved = apply(command, schedule);
        audit.success(command.teacherId(), AuditAction.CREATE, "TeachingPeriodSchedule", saved.getId(), null);
        return view(saved.getId());
    }

    @Override
    @UseCaseTransactional
    public ScheduledClassView update(AcademicCommands.SaveSchedule command) {
        TeachingPeriodSchedule schedule = requireOwned(command.teacherId(), command.teachingPeriodId(),
                command.scheduleId());
        apply(command, schedule);
        audit.success(command.teacherId(), AuditAction.UPDATE, "TeachingPeriodSchedule", schedule.getId(), null);
        return view(schedule.getId());
    }

    @Override
    @UseCaseTransactional
    public void delete(Long teacherId, Long teachingPeriodId, Long scheduleId) {
        requireOwned(teacherId, teachingPeriodId, scheduleId);
        schedules.deleteById(scheduleId);
        audit.success(teacherId, AuditAction.DELETE, "TeachingPeriodSchedule", scheduleId, null);
    }

    private TeachingPeriodSchedule apply(AcademicCommands.SaveSchedule command, TeachingPeriodSchedule schedule) {
        schedule.setDayOfWeek(command.dayOfWeek());
        schedule.setStartTime(command.startTime());
        schedule.setEndTime(command.endTime());
        schedule.setRoom(command.room());
        schedule.normalizeAndValidate();
        requireNoConflict(command.teacherId(), schedule);
        return schedules.save(schedule);
    }

    /** Un profesor no puede dictar dos clases a la vez; sólo cuentan periodos académicos que coexisten en fechas. */
    private void requireNoConflict(Long teacherId, TeachingPeriodSchedule schedule) {
        TeachingPeriod teachingPeriod = teachingPeriods.findById(schedule.getTeachingPeriodId())
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", schedule.getTeachingPeriodId()));
        AcademicPeriod period = academicPeriods.findById(teachingPeriod.getAcademicPeriodId())
                .orElseThrow(() -> ResourceNotFoundException.of("AcademicPeriod", teachingPeriod.getAcademicPeriodId()));
        schedules.findConflict(teacherId, schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime(),
                period.getStartDate(), period.getEndDate(), schedule.getId()).ifPresent(other -> {
            throw new ConflictException("SCHEDULE_CONFLICT", "The schedule overlaps with %s %s%s on %s %s-%s".formatted(
                    other.subjectName(), other.gradeName(), other.groupName(), other.dayOfWeek(), other.startTime(),
                    other.endTime()));
        });
    }

    private TeachingPeriodSchedule requireOwned(Long teacherId, Long teachingPeriodId, Long scheduleId) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        return schedules.findById(scheduleId)
                .filter(s -> s.getTeachingPeriodId().equals(teachingPeriodId))
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriodSchedule", scheduleId));
    }

    private ScheduledClassView view(Long scheduleId) {
        return schedules.findViewById(scheduleId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriodSchedule", scheduleId));
    }
}
