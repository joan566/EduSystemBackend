package com.edusistem.core.auth.infrastructure.adapter;

import static com.edusistem.core.shared.infrastructure.adapter.PersonalDataSql.strings;
import static com.edusistem.core.shared.infrastructure.adapter.PersonalDataSql.update;

import com.edusistem.core.auth.domain.outputports.AccountErasurePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Borra en orden de dependencias todo lo que pertenece al profesor. Todo cuelga de sus asignaciones docentes o de
 * tablas con teacher_id/user_id propio, y las FK compuestas garantizan que nada de otro profesor lo referencia.
 */
@Component
public class AccountErasureAdapter implements AccountErasurePort {

    private static final String TEACHING_PERIODS = """
            select tp.id from teaching_periods tp
            join teaching_assignments ta on ta.id = tp.teaching_assignment_id where ta.teacher_id = :user""";
    private static final String EVALUATIONS = "select e.id from evaluations e where e.teaching_period_id in ("
            + TEACHING_PERIODS + ")";
    private static final String EXAMS = "select x.id from exams x where x.evaluation_id in (" + EVALUATIONS + ")";

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<String> erase(Long userId, String email) {
        List<String> files = new ArrayList<>();
        files.addAll(strings(em, "select image_path from exam_submissions where exam_id in (" + EXAMS + ")", "user", userId));
        files.addAll(strings(em, "select storage_path from grade_attachments where evaluation_id in (" + EVALUATIONS + ")",
                "user", userId));
        files.addAll(strings(em, "select file_path from exam_submission_batches where teacher_id = :user", "user", userId));
        files.addAll(strings(em, "select file_path from import_batches where user_id = :user", "user", userId));
        files.addAll(strings(em, "select error_report_path from import_batches where user_id = :user", "user", userId));

        List<String> statements = List.of(
                // exámenes
                "delete from exam_answers where submission_id in (select id from exam_submissions where exam_id in ("
                        + EXAMS + "))",
                "delete from exam_submission_batches where teacher_id = :user", // páginas en cascada
                "delete from exam_submissions where exam_id in (" + EXAMS + ")",
                "delete from exam_question_options where question_id in (select id from exam_questions where exam_id in ("
                        + EXAMS + "))",
                "delete from exam_questions where exam_id in (" + EXAMS + ")",
                "delete from exams where evaluation_id in (" + EVALUATIONS + ")",
                // actividades y asistencia
                "delete from activity_grades where activity_id in (select id from activities where evaluation_id in ("
                        + EVALUATIONS + "))",
                "delete from activities where evaluation_id in (" + EVALUATIONS + ")",
                "delete from attendance_records where attendance_session_id in (select id from attendance_sessions "
                        + "where evaluation_id in (" + EVALUATIONS + "))",
                "delete from attendance_sessions where evaluation_id in (" + EVALUATIONS + ")",
                // evaluaciones (rúbricas, puntajes y adjuntos en cascada) y configuración de calificación
                "delete from evaluations where id in (" + EVALUATIONS + ")",
                "delete from grading_weights where grading_configuration_id in (select id from grading_configurations "
                        + "where teaching_period_id in (" + TEACHING_PERIODS + "))",
                "delete from grading_configurations where teaching_period_id in (" + TEACHING_PERIODS + ")",
                // clases (horarios y observaciones en cascada), estudiantes y catálogos
                "delete from teaching_periods where id in (" + TEACHING_PERIODS + ")",
                "delete from teaching_assignments where teacher_id = :user",
                "delete from student_groups where teacher_id = :user",
                "delete from students where teacher_id = :user",
                "delete from groups where teacher_id = :user",
                "delete from grades where teacher_id = :user",
                "delete from subjects where teacher_id = :user",
                "delete from academic_periods where teacher_id = :user",
                "delete from grading_scales where teacher_id = :user",
                // cuenta
                "delete from import_batches where user_id = :user",
                "delete from audit_logs where user_id = :user",
                "delete from refresh_tokens where user_id = :user",
                "delete from password_reset_tokens where user_id = :user",
                "delete from user_roles where user_id = :user",
                "delete from users where id = :user");
        for (String sql : statements) {
            update(em, sql, "user", userId);
        }
        // intentos de login fallidos (sin usuario asociado) que mencionan el correo
        update(em, "update audit_logs set details = null where user_id is null and position(:email in lower(details)) > 0",
                "email", email.toLowerCase(Locale.ROOT));
        return files;
    }
}
