package com.edusistem.core.student.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.inputports.EraseStudentUseCase;
import com.edusistem.core.student.domain.outputports.StudentErasurePort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudentErasureService implements EraseStudentUseCase {

    private static final Logger log = LoggerFactory.getLogger(StudentErasureService.class);

    private final StudentRepositoryPort students;
    private final StudentErasurePort erasure;
    private final FileStoragePort storage;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public StudentErasureService(StudentRepositoryPort students, StudentErasurePort erasure, FileStoragePort storage,
                                 OwnershipGuard guard, RecordAuditUseCase audit) {
        this.students = students;
        this.erasure = erasure;
        this.storage = storage;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public void erase(Long teacherId, Long studentId) {
        guard.requireStudent(teacherId, studentId);
        Student student = students.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Student", studentId));
        for (String path : erasure.erase(teacherId, student)) {
            try {
                storage.delete(path);
            } catch (IOException | RuntimeException e) {
                log.warn("Could not delete file {} of erased student {}", path, studentId, e);
            }
        }
        audit.success(teacherId, AuditAction.DELETE, "Student", studentId, "student and all their data erased");
    }
}
