package com.edusistem.core.student.domain.outputports;

import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.student.domain.entity.Student;
import java.util.List;
import java.util.Optional;

public interface StudentRepositoryPort {

    Student save(Student student);

    Optional<Student> findById(Long id);

    Optional<Student> findByStudentCode(String studentCode);

    Optional<Student> findByIdentificationNumber(String identificationNumber);

    /** Estudiantes visibles para el profesor (matriculados en grupos donde enseña), con filtros opcionales. */
    PageResult<Student> searchByTeacher(Long teacherId, Long groupId, String search, PageQuery page);

    /** Estudiantes con matrícula activa en el grupo, ordenados por apellido y nombre. */
    List<Student> findActiveByGroupId(Long groupId);
}
