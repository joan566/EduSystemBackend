package com.edusistem.core.student.infrastructure.adapter;

import com.edusistem.core.student.domain.outputports.StudentCodeGeneratorPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
public class SequenceStudentCodeGenerator implements StudentCodeGeneratorPort {

    @PersistenceContext
    private EntityManager em;

    @Override
    public String nextStudentCode() {
        Number next = (Number) em.createNativeQuery("select nextval('student_code_seq')").getSingleResult();
        return String.format("EST-%06d", next.longValue());
    }
}
