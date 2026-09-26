package com.edusistem.core.student.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.student.application.use_case.dtos.StudentCommands;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.entity.StudentGroup;
import com.edusistem.core.student.domain.inputports.QueryStudentUseCase;
import com.edusistem.core.student.domain.inputports.RegisterStudentUseCase;
import com.edusistem.core.student.domain.inputports.WithdrawStudentUseCase;
import com.edusistem.core.student.domain.outputports.StudentCodeGeneratorPort;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import com.edusistem.core.student.domain.vo.StudentDetails;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

public class StudentService implements RegisterStudentUseCase, QueryStudentUseCase, WithdrawStudentUseCase {

    private final StudentRepositoryPort students;
    private final StudentGroupRepositoryPort studentGroups;
    private final StudentCodeGeneratorPort codeGenerator;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;
    private final Clock clock;

    public StudentService(StudentRepositoryPort students, StudentGroupRepositoryPort studentGroups,
                          StudentCodeGeneratorPort codeGenerator, OwnershipGuard guard, RecordAuditUseCase audit,
                          Clock clock) {
        this.students = students;
        this.studentGroups = studentGroups;
        this.codeGenerator = codeGenerator;
        this.guard = guard;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @UseCaseTransactional
    public Student create(StudentCommands.Create command) {
        String identification = blankToNull(command.identificationNumber());
        String code = blankToNull(command.studentCode());
        if (identification != null && students.findByIdentificationNumber(identification).isPresent()) {
            throw new ConflictException("IDENTIFICATION_ALREADY_EXISTS",
                    "A student with identification number " + identification + " already exists");
        }
        if (code != null && students.findByStudentCode(code).isPresent()) {
            throw new ConflictException("STUDENT_CODE_ALREADY_EXISTS", "Student code " + code + " already exists");
        }
        return students.save(Student.builder()
                .identificationNumber(identification)
                .studentCode(code != null ? code : codeGenerator.nextStudentCode())
                .firstName(command.firstName().trim())
                .lastName(command.lastName().trim())
                .email(normalizeEmail(command.email()))
                .build());
    }

    @Override
    @UseCaseTransactional
    public Student update(StudentCommands.Update command) {
        Student student = students.findById(command.studentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Student", command.studentId()));
        student.setFirstName(command.firstName().trim());
        student.setLastName(command.lastName().trim());
        student.setEmail(normalizeEmail(command.email()));
        return students.save(student);
    }

    @Override
    @UseCaseTransactional
    public void enroll(StudentCommands.Enroll command) {
        LocalDateTime now = LocalDateTime.now(clock);
        StudentGroup enrollment = studentGroups.find(command.studentId(), command.groupId()).orElse(null);
        if (enrollment == null) {
            studentGroups.save(StudentGroup.builder().studentId(command.studentId()).groupId(command.groupId())
                    .enrolledAt(now).active(true).build());
        } else if (!enrollment.isActive()) {
            enrollment.reenroll(now);
            studentGroups.save(enrollment);
        }
    }

    @Override
    @UseCaseTransactional
    public void withdraw(StudentCommands.Withdraw command) {
        guard.requireStudent(command.teacherId(), command.studentId());
        guard.requireGroup(command.teacherId(), command.groupId());
        StudentGroup enrollment = studentGroups.find(command.studentId(), command.groupId())
                .orElseThrow(() -> ResourceNotFoundException.of("Enrollment", command.studentId() + "/" + command.groupId()));
        if (!enrollment.isActive()) {
            throw new ConflictException("STUDENT_ALREADY_WITHDRAWN", "The student is already withdrawn from the group");
        }
        enrollment.withdraw(LocalDateTime.now(clock));
        studentGroups.save(enrollment);
        audit.success(command.teacherId(), AuditAction.UPDATE, "StudentGroup", command.studentId(),
                "withdrawn from group " + command.groupId());
    }

    @Override
    public PageResult<Student> search(Long teacherId, Long groupId, String search, PageQuery page) {
        if (groupId != null) {
            guard.requireGroup(teacherId, groupId);
        }
        return students.searchByTeacher(teacherId, groupId, search, page);
    }

    @Override
    public StudentDetails get(Long teacherId, Long studentId) {
        guard.requireStudent(teacherId, studentId);
        Student student = students.findById(studentId).orElseThrow(() -> ResourceNotFoundException.of("Student", studentId));
        return new StudentDetails(student, studentGroups.findEnrollmentViews(studentId));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
