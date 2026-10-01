package com.edusistem.core.gradebook.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.audit.domain.vo.AuditTarget;
import com.edusistem.core.gradebook.application.contracts.EnrolledStudentLookup;
import com.edusistem.core.gradebook.application.use_case.dtos.GradebookCommands;
import com.edusistem.core.gradebook.domain.entity.StudentObservation;
import com.edusistem.core.gradebook.domain.inputports.ManageStudentObservationUseCase;
import com.edusistem.core.gradebook.domain.outputports.StudentObservationRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.student.domain.entity.Student;

/** Observación del docente sobre un estudiante en una clase (una por estudiante y teaching period). */
public class StudentObservationService implements ManageStudentObservationUseCase {

    static final int MAX_OBSERVATION_LENGTH = 1000;

    private final StudentObservationRepositoryPort observations;
    private final EnrolledStudentLookup enrolledStudents;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public StudentObservationService(StudentObservationRepositoryPort observations,
                                     EnrolledStudentLookup enrolledStudents, OwnershipGuard guard,
                                     RecordAuditUseCase audit) {
        this.observations = observations;
        this.enrolledStudents = enrolledStudents;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public StudentObservation save(GradebookCommands.SaveObservation command) {
        guard.requireTeachingPeriod(command.teacherId(), command.teachingPeriodId());
        guard.requireStudent(command.teacherId(), command.studentId());
        Student student = enrolledStudents.require(command.teachingPeriodId(), command.studentId());
        String text = command.text() == null ? "" : command.text().trim();
        if (text.length() > MAX_OBSERVATION_LENGTH) {
            throw new InvalidRequestException("OBSERVATION_TOO_LONG",
                    "The observation cannot exceed " + MAX_OBSERVATION_LENGTH + " characters");
        }
        StudentObservation existing = observations.find(command.teachingPeriodId(), command.studentId()).orElse(null);
        AuditTarget target = AuditTarget.inTeachingPeriod("StudentObservation", command.studentId(),
                command.teachingPeriodId(), AuditTarget.label("Observación", student.fullName()));
        if (text.isEmpty()) {
            if (existing != null) {
                observations.delete(existing.getId());
                audit.success(command.teacherId(), AuditAction.DELETE, target, "observation removed");
            }
            return null;
        }
        StudentObservation observation = existing != null ? existing : StudentObservation.builder()
                .teachingPeriodId(command.teachingPeriodId()).studentId(command.studentId()).build();
        observation.setText(text);
        StudentObservation saved = observations.save(observation);
        audit.success(command.teacherId(), existing == null ? AuditAction.CREATE : AuditAction.UPDATE, target,
                "observation saved");
        return saved;
    }
}
