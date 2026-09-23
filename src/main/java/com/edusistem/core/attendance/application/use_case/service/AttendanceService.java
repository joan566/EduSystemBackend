package com.edusistem.core.attendance.application.use_case.service;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.attendance.application.use_case.dtos.AttendanceCommands;
import com.edusistem.core.attendance.domain.entity.AttendanceRecord;
import com.edusistem.core.attendance.domain.entity.AttendanceSession;
import com.edusistem.core.attendance.domain.inputports.ManageAttendanceUseCase;
import com.edusistem.core.attendance.domain.outputports.AttendanceRecordRepositoryPort;
import com.edusistem.core.attendance.domain.outputports.AttendanceSessionRepositoryPort;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionDetails;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.attendance.domain.vo.StudentAttendanceView;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.application.use_case.dtos.EvaluationCommands;
import com.edusistem.core.evaluation.domain.entity.Evaluation;
import com.edusistem.core.evaluation.domain.enums.EvaluationCategoryCode;
import com.edusistem.core.evaluation.domain.inputports.CreateEvaluationUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService implements ManageAttendanceUseCase {

    private final AttendanceSessionRepositoryPort sessions;
    private final AttendanceRecordRepositoryPort records;
    private final EvaluationRepositoryPort evaluations;
    private final CreateEvaluationUseCase createEvaluation;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final StudentRepositoryPort students;
    private final StudentGroupRepositoryPort studentGroups;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public AttendanceService(AttendanceSessionRepositoryPort sessions, AttendanceRecordRepositoryPort records,
                             EvaluationRepositoryPort evaluations, CreateEvaluationUseCase createEvaluation,
                             TeachingPeriodRepositoryPort teachingPeriods, StudentRepositoryPort students,
                             StudentGroupRepositoryPort studentGroups, OwnershipGuard guard, RecordAuditUseCase audit) {
        this.sessions = sessions;
        this.records = records;
        this.evaluations = evaluations;
        this.createEvaluation = createEvaluation;
        this.teachingPeriods = teachingPeriods;
        this.students = students;
        this.studentGroups = studentGroups;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    @Transactional
    public AttendanceSessionView createSession(AttendanceCommands.CreateSession command) {
        if (command.sessionDate() == null) {
            throw new InvalidRequestException("INVALID_SESSION_DATE", "sessionDate is required");
        }
        String name = command.name() == null || command.name().isBlank() ? "Asistencia " + command.sessionDate()
                : command.name();
        BigDecimal max = command.maximumScore() != null ? command.maximumScore() : BigDecimal.ONE;
        Evaluation evaluation = createEvaluation.create(new EvaluationCommands.Create(command.teacherId(),
                command.teachingPeriodId(), EvaluationCategoryCode.ATTENDANCE, name, null,
                command.sessionDate().atStartOfDay(), max));
        AttendanceSession saved = sessions.save(AttendanceSession.builder().evaluationId(evaluation.getId())
                .sessionDate(command.sessionDate()).build());
        audit.success(command.teacherId(), AuditAction.CREATE, "AttendanceSession", saved.getId(), name);
        return sessions.findViewById(saved.getId()).orElseThrow();
    }

    @Override
    public AttendanceSessionDetails get(Long teacherId, Long sessionId) {
        guard.requireAttendanceSession(teacherId, sessionId);
        return details(sessionId);
    }

    @Override
    @Transactional
    public AttendanceSessionDetails recordAttendance(AttendanceCommands.RecordAttendance command) {
        guard.requireAttendanceSession(command.teacherId(), command.sessionId());
        if (command.records() == null || command.records().isEmpty()) {
            throw new InvalidRequestException("RECORDS_REQUIRED", "At least one attendance record is required");
        }
        AttendanceSessionView session = sessions.findViewById(command.sessionId())
                .orElseThrow(() -> ResourceNotFoundException.of("AttendanceSession", command.sessionId()));
        TeachingPeriodView period = teachingPeriods.findViewById(session.teachingPeriodId()).orElseThrow();
        Map<Long, AttendanceRecord> existing = records.findBySessionId(session.sessionId()).stream()
                .collect(Collectors.toMap(AttendanceRecord::getStudentId, Function.identity()));

        Set<Long> seen = new HashSet<>();
        List<AttendanceRecord> toSave = new ArrayList<>();
        for (AttendanceCommands.RecordInput input : command.records()) {
            if (input.studentId() == null || input.status() == null || !seen.add(input.studentId())) {
                throw new InvalidRequestException("INVALID_RECORDS",
                        "Each student must appear once with a studentId and a status");
            }
            if (!studentGroups.existsActive(input.studentId(), period.groupId())) {
                throw new InvalidRequestException("STUDENT_NOT_IN_GROUP",
                        "Student " + input.studentId() + " does not belong to the group of this session");
            }
            AttendanceRecord record = existing.getOrDefault(input.studentId(), AttendanceRecord.builder()
                    .attendanceSessionId(session.sessionId()).studentId(input.studentId()).build());
            record.setStatus(input.status());
            record.setObservation(input.observation() == null || input.observation().isBlank() ? null
                    : input.observation().trim());
            toSave.add(record);
        }
        records.saveAll(toSave);
        audit.success(command.teacherId(), AuditAction.UPDATE, "AttendanceSession", session.sessionId(),
                toSave.size() + " record(s) saved");
        return details(session.sessionId());
    }

    @Override
    @Transactional
    public void delete(Long teacherId, Long sessionId) {
        guard.requireAttendanceSession(teacherId, sessionId);
        AttendanceSession session = sessions.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("AttendanceSession", sessionId));
        if (records.existsBySessionId(sessionId)) {
            throw new ConflictException("ATTENDANCE_SESSION_HAS_RECORDS",
                    "The session has attendance records and cannot be deleted");
        }
        sessions.deleteById(sessionId);
        evaluations.deleteById(session.getEvaluationId());
        audit.success(teacherId, AuditAction.DELETE, "AttendanceSession", sessionId, null);
    }

    @Override
    public PageResult<AttendanceSessionView> search(Long teacherId, Long teachingPeriodId, PageQuery page) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        return sessions.findViewsByTeachingPeriodId(teachingPeriodId, page);
    }

    private AttendanceSessionDetails details(Long sessionId) {
        AttendanceSessionView session = sessions.findViewById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("AttendanceSession", sessionId));
        TeachingPeriodView period = teachingPeriods.findViewById(session.teachingPeriodId()).orElseThrow();
        Map<Long, AttendanceRecord> byStudent = records.findBySessionId(sessionId).stream()
                .collect(Collectors.toMap(AttendanceRecord::getStudentId, Function.identity()));
        List<StudentAttendanceView> views = new ArrayList<>();
        for (Student s : students.findActiveByGroupId(period.groupId())) {
            AttendanceRecord r = byStudent.get(s.getId());
            views.add(new StudentAttendanceView(s.getId(), s.getStudentCode(), s.fullName(),
                    r == null ? null : r.getStatus(), r == null ? null : r.getObservation()));
        }
        return new AttendanceSessionDetails(session, views);
    }
}
