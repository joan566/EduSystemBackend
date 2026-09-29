package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Notas por estudiante evaluación por evaluación, nota mínima, rúbricas, adjuntos y observaciones. */
class GradebookIntegrationTest extends IntegrationTest {

    private record Scenario(Teacher teacher, Context context, Student student, Student other, long activityId,
                            long activityEvaluationId, long projectEvaluationId, long sessionEvaluationId) {
    }

    /** Taller (máx 5, nota 4), Proyecto (máx 10, nota 5) y dos sesiones (presente, ausente) para el estudiante 1. */
    private Scenario scenario() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        List<Student> students = importStudents(t, c, 2);
        Student s1 = students.get(0);
        JsonNode taller = post(t, "/api/v1/activities", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "Taller",
                "maximumScore", 5, "activityType", "Tarea", "description", "Lectura crítica"), 201);
        JsonNode proyecto = post(t, "/api/v1/activities", Map.of("teachingPeriodId", c.teachingPeriodId(),
                "name", "Proyecto", "maximumScore", 10), 201);
        put(t, "/api/v1/activities/" + taller.get("id").asLong() + "/grades",
                Map.of("grades", List.of(Map.of("studentId", s1.id(), "grade", 4, "comment", "Buen trabajo"))), 200);
        put(t, "/api/v1/activities/" + proyecto.get("id").asLong() + "/grades",
                Map.of("grades", List.of(Map.of("studentId", s1.id(), "grade", 5))), 200);
        long sessionEvaluation = 0;
        for (int day = 1; day <= 2; day++) {
            JsonNode session = post(t, "/api/v1/attendance-sessions", Map.of("teachingPeriodId", c.teachingPeriodId(),
                    "sessionDate", YEAR + "-02-0" + day), 201);
            sessionEvaluation = session.get("evaluationId").asLong();
            put(t, "/api/v1/attendance-sessions/" + session.get("id").asLong() + "/records", Map.of("records",
                    List.of(Map.of("studentId", s1.id(), "status", day == 1 ? "PRESENT" : "ABSENT"))), 200);
        }
        return new Scenario(t, c, s1, students.get(1), taller.get("id").asLong(), taller.get("evaluationId").asLong(),
                proyecto.get("evaluationId").asLong(), sessionEvaluation);
    }

    private String reportUrl(Scenario s, Student student) {
        return "/api/v1/teaching-periods/" + s.context().teachingPeriodId() + "/students/" + student.id() + "/grade-report";
    }

    private JsonNode entry(JsonNode report, long evaluationId) {
        for (JsonNode e : report.get("evaluations")) {
            if (e.get("evaluationId").asLong() == evaluationId) {
                return e;
            }
        }
        throw new AssertionError("evaluation not in report");
    }

    @Test
    void reportSplitsCategoryWeightsPerEvaluationAndMatchesThePeriodGrade() {
        Scenario s = scenario();
        JsonNode report = get(s.teacher(), reportUrl(s, s.student()), 200);

        // Actividades pesan 20 %: 15 puntos máximos, así que Taller (5) pesa 6.67 y aporta 20×4/15 = 5.33.
        JsonNode taller = entry(report, s.activityEvaluationId());
        assertThat(taller.get("type").asText()).isEqualTo("ACTIVITY");
        assertThat(taller.get("weight").decimalValue()).isEqualByComparingTo("6.67");
        assertThat(taller.get("contribution").decimalValue()).isEqualByComparingTo("5.33");
        assertThat(taller.get("comment").asText()).isEqualTo("Buen trabajo");
        assertThat(taller.get("description").asText()).isEqualTo("Lectura crítica");
        assertThat(entry(report, s.projectEvaluationId()).get("weight").decimalValue()).isEqualByComparingTo("13.33");
        JsonNode absent = entry(report, s.sessionEvaluationId());
        assertThat(absent.get("type").asText()).isEqualTo("ATTENDANCE");
        assertThat(absent.get("contribution").decimalValue()).isEqualByComparingTo("0");

        // 12 (actividades) + 10 (asistencia) + 0 (sin exámenes) = 22 puntos -> 0.22 × 5 = 1.10
        assertThat(report.get("score").decimalValue()).isEqualByComparingTo("22");
        assertThat(report.get("periodGrade").decimalValue()).isEqualByComparingTo("1.10");
        JsonNode period = get(s.teacher(), "/api/v1/teaching-periods/" + s.context().teachingPeriodId() + "/period-grades", 200);
        for (JsonNode row : period.get("students")) {
            if (row.get("studentId").asLong() == s.student().id()) {
                assertThat(row.get("periodGrade").decimalValue()).isEqualByComparingTo(report.get("periodGrade").decimalValue());
                assertThat(row.get("passing").isNull()).isTrue();
            }
        }
        assertThat(report.get("passing").isNull()).isTrue();
        assertThat(report.get("student").get("id").asLong()).isEqualTo(s.student().id());

        Teacher stranger = newTeacher();
        assertError(get(stranger, reportUrl(s, s.student()), 404), 404, "RESOURCE_NOT_FOUND");
    }

    @Test
    void passingGradeIsValidatedAgainstTheScaleAndDrivesTheStatus() {
        Scenario s = scenario();
        String url = "/api/v1/teaching-periods/" + s.context().teachingPeriodId() + "/grading-configuration";
        JsonNode config = get(s.teacher(), url, 200);
        List<Map<String, Object>> weights = new java.util.ArrayList<>();
        config.get("weights").forEach(w -> weights.add(Map.of("evaluationCategoryId", w.get("evaluationCategoryId").asLong(),
                "weight", w.get("weight").decimalValue())));
        long scale = config.get("scale").get("id").asLong();

        assertError(put(s.teacher(), url, Map.of("gradingScaleId", scale, "weights", weights, "passingGrade", 6), 400),
                400, "INVALID_PASSING_GRADE");
        JsonNode saved = put(s.teacher(), url, Map.of("gradingScaleId", scale, "weights", weights, "passingGrade", 1), 200);
        assertThat(saved.get("passingGrade").decimalValue()).isEqualByComparingTo("1");

        JsonNode report = get(s.teacher(), reportUrl(s, s.student()), 200);
        assertThat(report.get("passing").asBoolean()).isTrue(); // 1.10 >= 1
        JsonNode period = get(s.teacher(), "/api/v1/teaching-periods/" + s.context().teachingPeriodId() + "/period-grades", 200);
        assertThat(period.get("passingGrade").decimalValue()).isEqualByComparingTo("1");
        for (JsonNode row : period.get("students")) {
            assertThat(row.get("passing").asBoolean()).isEqualTo(row.get("studentId").asLong() == s.student().id());
        }
    }

    @Test
    void rubricScoresSetTheActivityGradeAndSurviveRenamingCriteria() {
        Scenario s = scenario();
        String rubricUrl = "/api/v1/evaluations/" + s.activityEvaluationId() + "/rubric";
        assertError(put(s.teacher(), rubricUrl, Map.of("criteria", List.of(Map.of("name", "A", "weight", 60),
                Map.of("name", "B", "weight", 30))), 400), 400, "RUBRIC_WEIGHTS_MUST_SUM_100");
        assertError(put(s.teacher(), "/api/v1/evaluations/" + s.sessionEvaluationId() + "/rubric",
                Map.of("criteria", List.of(Map.of("name", "A", "weight", 100))), 409), 409, "RUBRIC_NOT_SUPPORTED");

        JsonNode criteria = put(s.teacher(), rubricUrl, Map.of("criteria", List.of(
                Map.of("name", "Coherencia", "weight", 60), Map.of("name", "Ortografía", "weight", 40))), 200);
        long coherence = criteria.get(0).get("id").asLong();
        long spelling = criteria.get(1).get("id").asLong();

        String scoresUrl = "/api/v1/evaluations/" + s.activityEvaluationId() + "/students/" + s.student().id() + "/rubric-scores";
        assertError(put(s.teacher(), scoresUrl, Map.of("scores", List.of(Map.of("criterionId", coherence, "score", 4))), 400),
                400, "INVALID_RUBRIC_SCORES");
        assertError(put(s.teacher(), scoresUrl, Map.of("scores", List.of(Map.of("criterionId", coherence, "score", 6),
                Map.of("criterionId", spelling, "score", 5))), 400), 400, "INVALID_RUBRIC_SCORES");
        JsonNode detail = put(s.teacher(), scoresUrl, Map.of("scores", List.of(Map.of("criterionId", coherence, "score", 4),
                Map.of("criterionId", spelling, "score", 5))), 200);
        // 4 × 0.6 + 5 × 0.4 = 4.4; sin comentario en la petición se conserva el anterior
        assertThat(detail.get("evaluation").get("earned").decimalValue()).isEqualByComparingTo("4.4");
        assertThat(detail.get("evaluation").get("comment").asText()).isEqualTo("Buen trabajo");
        assertThat(detail.get("evaluation").get("hasRubric").asBoolean()).isTrue();
        assertThat(detail.get("rubric").get(1).get("score").decimalValue()).isEqualByComparingTo("5");
        assertThat(jdbc.queryForObject("select grade from activity_grades where activity_id = ? and student_id = ?",
                BigDecimal.class, s.activityId(), s.student().id())).isEqualByComparingTo("4.4");

        // Renombrar un criterio conserva su puntaje; quitar otro borra el suyo.
        put(s.teacher(), rubricUrl, Map.of("criteria", List.of(Map.of("id", coherence, "name", "Coherencia y estructura",
                "weight", 100))), 200);
        JsonNode after = get(s.teacher(), "/api/v1/evaluations/" + s.activityEvaluationId() + "/students/"
                + s.student().id() + "/grade-detail", 200);
        assertThat(after.get("rubric").size()).isEqualTo(1);
        assertThat(after.get("rubric").get(0).get("name").asText()).isEqualTo("Coherencia y estructura");
        assertThat(after.get("rubric").get(0).get("score").decimalValue()).isEqualByComparingTo("4");

        call("DELETE", s.teacher(), rubricUrl, null, 204);
        assertThat(get(s.teacher(), rubricUrl, 200).size()).isZero();
    }

    @Test
    void attachmentsAreStoredPerGradeAndObservationsPerStudent() throws Exception {
        Scenario s = scenario();
        String url = "/api/v1/evaluations/" + s.activityEvaluationId() + "/students/" + s.student().id() + "/attachment";
        byte[] content = "texto del estudiante".getBytes();

        MvcResult rejected = upload(s.teacher(), url, "file", "virus.exe", "application/octet-stream", content, Map.of());
        assertThat(rejected.getResponse().getStatus()).isEqualTo(400);
        MvcResult attendance = upload(s.teacher(), "/api/v1/evaluations/" + s.sessionEvaluationId() + "/students/"
                + s.student().id() + "/attachment", "file", "a.pdf", "application/pdf", content, Map.of());
        assertThat(attendance.getResponse().getStatus()).isEqualTo(409);

        MvcResult uploaded = upload(s.teacher(), url, "file", "Texto_Mateo.docx", "application/octet-stream", content, Map.of());
        JsonNode attachment = parse(uploaded, 200);
        assertThat(attachment.get("fileName").asText()).isEqualTo("Texto_Mateo.docx");
        assertThat(attachment.get("contentType").asText()).contains("wordprocessingml");
        assertThat(attachment.get("sizeBytes").asLong()).isEqualTo(content.length);
        assertThat(entry(get(s.teacher(), reportUrl(s, s.student()), 200), s.activityEvaluationId())
                .get("hasAttachment").asBoolean()).isTrue();

        MvcResult file = download(s.teacher(), url);
        assertThat(file.getResponse().getStatus()).isEqualTo(200);
        assertThat(file.getResponse().getContentAsByteArray()).isEqualTo(content);
        assertThat(file.getResponse().getHeader("Content-Disposition")).contains("Texto_Mateo.docx");

        call("DELETE", s.teacher(), url, null, 204);
        assertError(get(s.teacher(), url, 404), 404, "ATTACHMENT_NOT_FOUND");

        String observationUrl = "/api/v1/teaching-periods/" + s.context().teachingPeriodId() + "/students/"
                + s.student().id() + "/observation";
        assertThat(put(s.teacher(), observationUrl, Map.of("text", "  Excelente desempeño.  "), 200).get("text").asText())
                .isEqualTo("Excelente desempeño.");
        assertThat(get(s.teacher(), reportUrl(s, s.student()), 200).get("observation").get("text").asText())
                .isEqualTo("Excelente desempeño.");
        call("PUT", s.teacher(), observationUrl, Map.of("text", " "), 204);
        assertThat(get(s.teacher(), reportUrl(s, s.student()), 200).get("observation").isNull()).isTrue();
        assertThat(get(s.teacher(), reportUrl(s, s.other()), 200).get("observation").isNull()).isTrue();
    }
}
