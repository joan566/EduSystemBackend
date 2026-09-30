package com.edusistem.core.student.infrastructure.adapter;

import static com.edusistem.core.shared.infrastructure.adapter.PersonalDataSql.ERASED_STUDENT;
import static com.edusistem.core.shared.infrastructure.adapter.PersonalDataSql.scrubAudit;
import static com.edusistem.core.shared.infrastructure.adapter.PersonalDataSql.strings;
import static com.edusistem.core.shared.infrastructure.adapter.PersonalDataSql.update;

import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentErasurePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StudentErasureAdapter implements StudentErasurePort {

    private static final String SUBMISSIONS = "select id from exam_submissions where student_id = :student";

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<String> erase(Long teacherId, Student student) {
        Long id = student.getId();
        List<String> files = new ArrayList<>();
        files.addAll(strings(em, "select image_path from exam_submissions where student_id = :student", "student", id));
        files.addAll(strings(em, "select storage_path from grade_attachments where student_id = :student", "student", id));

        // páginas de lotes escaneados: conservan el resultado del lote pero no el código del estudiante
        update(em, """
                update exam_submission_batch_pages set student_code = null
                where submission_id in (""" + SUBMISSIONS + """
                ) or (student_code = :code
                      and batch_id in (select id from exam_submission_batches where teacher_id = :teacher))""",
                "student", id, "code", student.getStudentCode(), "teacher", teacherId);
        update(em, "delete from exam_answers where submission_id in (" + SUBMISSIONS + ")", "student", id);
        for (String table : List.of("exam_submissions", "activity_grades", "attendance_records", "rubric_scores",
                "grade_attachments", "student_observations", "student_groups")) {
            update(em, "delete from " + table + " where student_id = :student", "student", id);
        }
        update(em, "delete from students where id = :student and teacher_id = :teacher", "student", id,
                "teacher", teacherId);

        scrubAudit(em, teacherId, student.getFirstName() + " " + student.getLastName(), ERASED_STUDENT);
        scrubAudit(em, teacherId, student.fullName(), ERASED_STUDENT);
        scrubAudit(em, teacherId, student.getStudentCode(), ERASED_STUDENT);
        scrubAudit(em, teacherId, student.getIdentificationNumber(), ERASED_STUDENT);
        update(em, """
                update audit_logs set entity_label = null
                where user_id = :teacher and entity_type in ('Student', 'StudentGroup') and entity_id = :student""",
                "teacher", teacherId, "student", id);
        return files;
    }
}
