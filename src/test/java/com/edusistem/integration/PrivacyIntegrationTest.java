package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.core.imports.domain.inputports.PurgeImportFilesUseCase;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Supresión de datos (estudiante y cuenta completa) y retención de archivos con datos personales. */
class PrivacyIntegrationTest extends IntegrationTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private FileStoragePort storage;
    @Autowired
    private PurgeImportFilesUseCase importFilePurge;

    private record Scenario(Teacher teacher, Context context, Student erased, Student kept, String attachmentPath) {
    }

    /** Dos estudiantes; el primero con nota, asistencia, observación y adjunto. */
    private Scenario scenario() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        List<Student> students = importStudents(t, c, 2);
        Student s1 = students.get(0);
        Student s2 = students.get(1);
        JsonNode activity = post(t, "/api/v1/activities", Map.of("teachingPeriodId", c.teachingPeriodId(),
                "name", "Taller", "maximumScore", 5), 201);
        put(t, "/api/v1/activities/" + activity.get("id").asLong() + "/grades",
                Map.of("grades", List.of(Map.of("studentId", s1.id(), "grade", 4))), 200);
        put(t, "/api/v1/activities/" + activity.get("id").asLong() + "/grades",
                Map.of("grades", List.of(Map.of("studentId", s2.id(), "grade", 3))), 200);
        JsonNode session = post(t, "/api/v1/attendance-sessions", Map.of("teachingPeriodId", c.teachingPeriodId(),
                "sessionDate", YEAR + "-02-01"), 201);
        put(t, "/api/v1/attendance-sessions/" + session.get("id").asLong() + "/records",
                Map.of("records", List.of(Map.of("studentId", s1.id(), "status", "PRESENT"))), 200);
        put(t, "/api/v1/teaching-periods/" + c.teachingPeriodId() + "/students/" + s1.id() + "/observation",
                Map.of("text", "Muy participativo"), 200);
        long evaluationId = activity.get("evaluationId").asLong();
        parse(upload(t, "/api/v1/evaluations/" + evaluationId + "/students/" + s1.id() + "/attachment", "file",
                "trabajo.pdf", "application/pdf", "contenido".getBytes(), Map.of()), 200);
        String path = jdbc.queryForObject("select storage_path from grade_attachments where student_id = ?", String.class,
                s1.id());
        return new Scenario(t, c, s1, s2, path);
    }

    private int count(String table, long studentId) {
        return jdbc.queryForObject("select count(*) from " + table + " where student_id = ?", Integer.class, studentId);
    }

    @Test
    void erasingAStudentRemovesAllTheirDataFilesAndMentionsInTheAudit() {
        Scenario s = scenario();
        assertThat(storage.exists(s.attachmentPath())).isTrue();
        String auditBefore = get(s.teacher(), "/api/v1/audit-logs?size=100", 200).toString();
        assertThat(auditBefore).contains("Apellido1");

        // otro profesor no puede borrarlo
        assertError(call("DELETE", newTeacher(), "/api/v1/students/" + s.erased().id(), null, 404), 404,
                "RESOURCE_NOT_FOUND");

        call("DELETE", s.teacher(), "/api/v1/students/" + s.erased().id(), null, 204);

        assertError(get(s.teacher(), "/api/v1/students/" + s.erased().id(), 404), 404, "RESOURCE_NOT_FOUND");
        for (String table : List.of("activity_grades", "attendance_records", "student_observations", "grade_attachments",
                "student_groups", "exam_submissions")) {
            assertThat(count(table, s.erased().id())).as(table).isZero();
        }
        assertThat(jdbc.queryForObject("select count(*) from students where id = ?", Integer.class, s.erased().id())).isZero();
        assertThat(storage.exists(s.attachmentPath())).isFalse();

        // la auditoría se conserva pero sin el nombre, código ni identificación del estudiante
        String auditAfter = jdbc.queryForList("select coalesce(entity_label, '') || ' ' || coalesce(details, '') "
                + "from audit_logs where user_id = ?", String.class, s.teacher().id()).toString();
        assertThat(auditAfter).doesNotContain("Nombre1 Apellido1").doesNotContain("Apellido1 Nombre1")
                .doesNotContain(s.erased().code()).doesNotContain(s.erased().identification())
                .contains("[estudiante eliminado]");

        // el otro estudiante sigue intacto
        get(s.teacher(), "/api/v1/students/" + s.kept().id(), 200);
        assertThat(count("activity_grades", s.kept().id())).isEqualTo(1);
        assertThat(auditAfter).contains("Nombre2 Apellido2");
    }

    @Test
    void deletingTheAccountRemovesEveryPieceOfTheTeachersDataButNotOtherTeachers() {
        Scenario s = scenario();
        Scenario other = scenario();
        long teacherId = s.teacher().id();

        call("DELETE", s.teacher(), "/api/v1/auth/account", Map.of("password", PASSWORD), 204);

        for (String sql : List.of(
                "select count(*) from users where id = ?",
                "select count(*) from students where teacher_id = ?",
                "select count(*) from groups where teacher_id = ?",
                "select count(*) from grades where teacher_id = ?",
                "select count(*) from subjects where teacher_id = ?",
                "select count(*) from academic_periods where teacher_id = ?",
                "select count(*) from teaching_assignments where teacher_id = ?",
                "select count(*) from import_batches where user_id = ?",
                "select count(*) from audit_logs where user_id = ?",
                "select count(*) from refresh_tokens where user_id = ?")) {
            assertThat(jdbc.queryForObject(sql, Integer.class, teacherId)).as(sql).isZero();
        }
        assertThat(jdbc.queryForObject("select count(*) from teaching_periods where id = ?", Integer.class,
                s.context().teachingPeriodId())).isZero();
        assertThat(storage.exists(s.attachmentPath())).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where details like ?", Integer.class,
                "%" + s.teacher().email() + "%")).isZero();

        // el otro profesor conserva todo
        assertThat(get(other.teacher(), "/api/v1/students?groupId=" + other.context().groupId(), 200)
                .get("totalElements").asInt()).isEqualTo(2);
        assertThat(storage.exists(other.attachmentPath())).isTrue();
    }

    @Test
    void importedSpreadsheetsAndErrorReportsAreDeletedAfterTheRetentionPeriod() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        byte[] file = xlsx(IMPORT_HEADERS, List.of(
                List.of(unique("ID"), "Ana", "Pérez", "", c.gradeName(), c.groupName(), YEAR),
                List.of("", "", "", "", c.gradeName(), c.groupName(), YEAR))); // fila con errores
        long batchId = parse(upload(t, "/api/v1/imports/students", "file", "s.xlsx", XLSX, file, Map.of()), 201)
                .get("id").asLong();
        String filePath = jdbc.queryForObject("select file_path from import_batches where id = ?", String.class, batchId);
        String reportPath = jdbc.queryForObject("select error_report_path from import_batches where id = ?", String.class,
                batchId);
        assertThat(storage.exists(filePath)).isTrue();
        assertThat(storage.exists(reportPath)).isTrue();

        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(30);
        assertThat(importFilePurge.purgeFilesCompletedBefore(cutoff)).isZero(); // aún dentro del plazo
        jdbc.update("update import_batches set completed_at = now() - interval '40 days' where id = ?", batchId);
        assertThat(importFilePurge.purgeFilesCompletedBefore(cutoff)).isGreaterThanOrEqualTo(1);

        assertThat(storage.exists(filePath)).isFalse();
        assertThat(storage.exists(reportPath)).isFalse();
        JsonNode batch = get(t, "/api/v1/imports/" + batchId, 200);
        assertThat(batch.get("successfulRows").asInt()).isEqualTo(1); // el resumen se conserva
        assertError(get(t, "/api/v1/imports/" + batchId + "/error-report", 404), 404, "ERROR_REPORT_NOT_FOUND");
    }
}
