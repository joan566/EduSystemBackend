package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Escalas, ponderaciones, actividades, asistencia y cálculo de la nota del periodo. */
class EvaluationsIntegrationTest extends IntegrationTest {

    private Map<String, Object> weights(Object exams, Object activities, Object attendance, long scaleId) {
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        if (exams != null) {
            list.add(Map.of("evaluationCategoryId", categoryId("EXAMS"), "weight", exams));
        }
        if (activities != null) {
            list.add(Map.of("evaluationCategoryId", categoryId("ACTIVITIES"), "weight", activities));
        }
        if (attendance != null) {
            list.add(Map.of("evaluationCategoryId", categoryId("ATTENDANCE"), "weight", attendance));
        }
        return Map.of("gradingScaleId", scaleId, "weights", list);
    }

    @Test
    void customScalesAreValidated() {
        Teacher t = newTeacher();
        JsonNode created = post(t, "/api/v1/grading-scales", Map.of("name", unique("1-7 "), "minimumValue", 1, "maximumValue", 7), 201);
        assertThat(created.get("maximumValue").decimalValue()).isEqualByComparingTo("7");
        assertError(post(t, "/api/v1/grading-scales", Map.of("name", "bad", "minimumValue", 5, "maximumValue", 5), 400), 400, "INVALID_GRADING_SCALE");
        assertError(post(t, "/api/v1/grading-scales", Map.of("name", "bad", "minimumValue", 10, "maximumValue", 0), 400), 400, "INVALID_GRADING_SCALE");
        assertThat(get(t, "/api/v1/grading-scales", 200).size()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void weightPolicyAllowsPartialConfigurationButRequires100ToCalculate() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        String url = "/api/v1/teaching-periods/" + c.teachingPeriodId() + "/grading-configuration";
        long scale5 = scaleId("Colombian 0-5");

        JsonNode valid = put(t, url, weights(60, 20, 20, scale5), 200);
        assertThat(valid.get("complete").asBoolean()).isTrue();
        assertThat(valid.get("totalWeight").decimalValue()).isEqualByComparingTo("100");

        assertError(put(t, url, weights(60, 20, 30, scale5), 400), 400, "WEIGHTS_EXCEED_100");
        assertError(put(t, url, weights(-10, 20, 30, scale5), 400), 400, "INVALID_REQUEST");
        assertError(put(t, url, weights(101, null, null, scale5), 400), 400, "INVALID_REQUEST");

        JsonNode partial = put(t, url, weights(60, 20, null, scale5), 200);
        assertThat(partial.get("complete").asBoolean()).isFalse();
        assertThat(get(t, url, 200).get("totalWeight").decimalValue()).isEqualByComparingTo("80");
        assertError(get(t, "/api/v1/teaching-periods/" + c.teachingPeriodId() + "/period-grades", 422), 422, "GRADING_CONFIGURATION_INCOMPLETE");
    }

    @Test
    void periodGradeCombinesActivitiesAndAttendanceUsingConfiguredWeightsAndScale() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        List<Student> students = importStudents(t, c, 2);
        Student s1 = students.get(0);
        Student s2 = students.get(1);

        long activity = post(t, "/api/v1/activities", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "Taller 1",
                "maximumScore", 5, "activityType", "Taller"), 201).get("id").asLong();
        put(t, "/api/v1/activities/" + activity + "/grades", Map.of("grades", List.of(
                Map.of("studentId", s1.id(), "grade", 4, "comment", "Bien"),
                Map.of("studentId", s2.id(), "grade", 2.5))), 200);

        for (int day = 1; day <= 2; day++) {
            long session = post(t, "/api/v1/attendance-sessions", Map.of("teachingPeriodId", c.teachingPeriodId(),
                    "sessionDate", YEAR + "-02-0" + day), 201).get("id").asLong();
            put(t, "/api/v1/attendance-sessions/" + session + "/records", Map.of("records", List.of(
                    Map.of("studentId", s1.id(), "status", day == 1 ? "PRESENT" : "ABSENT"),
                    Map.of("studentId", s2.id(), "status", day == 1 ? "PRESENT" : "EXCUSED"))), 200);
        }

        JsonNode report = get(t, "/api/v1/teaching-periods/" + c.teachingPeriodId() + "/period-grades", 200);
        assertThat(report.get("scale").get("maximumValue").decimalValue()).isEqualByComparingTo("5");
        // s1: 0.2*(4/5) + 0.2*(1/2) = 0.26 -> 1.30 ; s2: 0.2*(2.5/5) + 0.2*(1/1, excusa no cuenta) = 0.30 -> 1.50 ; sin exámenes = 0
        assertThat(gradeOf(report, s1)).isEqualByComparingTo("1.30");
        assertThat(gradeOf(report, s2)).isEqualByComparingTo("1.50");
        assertThat(report.get("students").get(0).get("categories").size()).isEqualTo(3);

        // la misma configuración sobre otra escala no cambia la lógica: sólo la escala
        put(t, "/api/v1/teaching-periods/" + c.teachingPeriodId() + "/grading-configuration",
                weights(60, 20, 20, scaleId("Percentage 0-100")), 200);
        JsonNode percent = get(t, "/api/v1/teaching-periods/" + c.teachingPeriodId() + "/period-grades", 200);
        assertThat(gradeOf(percent, s1)).isEqualByComparingTo("26.00");
        assertThat(gradeOf(percent, s2)).isEqualByComparingTo("30.00");
    }

    private java.math.BigDecimal gradeOf(JsonNode report, Student student) {
        for (JsonNode s : report.get("students")) {
            if (s.get("studentId").asLong() == student.id()) {
                return s.get("periodGrade").decimalValue();
            }
        }
        throw new AssertionError("student not in report");
    }

    @Test
    void activityGradesAreValidatedAgainstTheMaximumScoreAndTheGroup() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        List<Student> students = importStudents(t, c, 2);
        Context other = newContext(t);
        Student outsider = importStudents(t, other, 1).get(0);
        long activity = post(t, "/api/v1/activities", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "Proyecto",
                "maximumScore", 10), 201).get("id").asLong();
        String url = "/api/v1/activities/" + activity + "/grades";

        assertError(put(t, url, Map.of("grades", List.of(Map.of("studentId", students.get(0).id(), "grade", 10.5))), 400), 400, "GRADE_OUT_OF_RANGE");
        assertError(put(t, url, Map.of("grades", List.of(Map.of("studentId", students.get(0).id(), "grade", -1))), 400), 400, "INVALID_REQUEST");
        assertError(put(t, url, Map.of("grades", List.of(Map.of("studentId", outsider.id(), "grade", 5))), 400), 400, "STUDENT_NOT_IN_GROUP");
        assertError(put(t, url, Map.of("grades", List.of(Map.of("studentId", students.get(0).id(), "grade", 5),
                Map.of("studentId", students.get(0).id(), "grade", 6))), 400), 400, "INVALID_GRADES");

        // atomicidad: una nota inválida en el lote no guarda ninguna
        assertError(put(t, url, Map.of("grades", List.of(Map.of("studentId", students.get(0).id(), "grade", 5),
                Map.of("studentId", students.get(1).id(), "grade", 11))), 400), 400, "GRADE_OUT_OF_RANGE");
        assertThat(jdbc.queryForObject("select count(*) from activity_grades where activity_id = ?", Integer.class, activity)).isZero();

        put(t, url, Map.of("grades", List.of(Map.of("studentId", students.get(0).id(), "grade", 7, "comment", "ok"))), 200);
        put(t, url + "/" + students.get(0).id(), Map.of("grade", 9), 200); // corrige la misma nota
        assertThat(jdbc.queryForObject("select count(*) from activity_grades where activity_id = ?", Integer.class, activity)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select grade from activity_grades where activity_id = ?", java.math.BigDecimal.class, activity))
                .isEqualByComparingTo("9");
        JsonNode listed = get(t, url, 200);
        assertThat(listed.size()).isEqualTo(2);
        assertThat(listed.toString()).contains("null"); // el estudiante sin calificar aparece con grade nulo

        assertError(call("DELETE", t, "/api/v1/activities/" + activity, null, 409), 409, "ACTIVITY_HAS_GRADES");
        assertError(put(t, "/api/v1/activities/" + activity, Map.of("name", "Proyecto", "maximumScore", 5), 409), 409, "MAXIMUM_SCORE_BELOW_GRADES");
        assertThat(get(t, "/api/v1/audit-logs?action=GRADE_UPDATED", 200).get("totalElements").asInt()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void attendanceKeepsOneRecordPerStudentAndSession() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        Student s = importStudents(t, c, 1).get(0);
        Student outsider = importStudents(t, newContext(t), 1).get(0);
        long session = post(t, "/api/v1/attendance-sessions", Map.of("teachingPeriodId", c.teachingPeriodId(),
                "sessionDate", YEAR + "-03-10", "name", "Asistencia semana 1"), 201).get("id").asLong();
        String url = "/api/v1/attendance-sessions/" + session + "/records";

        put(t, url, Map.of("records", List.of(Map.of("studentId", s.id(), "status", "ABSENT", "observation", "gripa"))), 200);
        JsonNode updated = put(t, url, Map.of("records", List.of(Map.of("studentId", s.id(), "status", "EXCUSED"))), 200);
        assertThat(jdbc.queryForObject("select count(*) from attendance_records where attendance_session_id = ?", Integer.class, session)).isEqualTo(1);
        assertThat(updated.get("students").get(0).get("status").asText()).isEqualTo("EXCUSED");

        assertError(put(t, url, Map.of("records", List.of(Map.of("studentId", s.id(), "status", "LATE"))), 400), 400, "INVALID_REQUEST");
        assertError(put(t, url, Map.of("records", List.of(Map.of("studentId", outsider.id(), "status", "PRESENT"))), 400), 400, "STUDENT_NOT_IN_GROUP");
        assertError(call("DELETE", t, "/api/v1/attendance-sessions/" + session, null, 409), 409, "ATTENDANCE_SESSION_HAS_RECORDS");

        long empty = post(t, "/api/v1/attendance-sessions", Map.of("teachingPeriodId", c.teachingPeriodId(), "sessionDate", YEAR + "-03-11"), 201)
                .get("id").asLong();
        call("DELETE", t, "/api/v1/attendance-sessions/" + empty, null, 204);
    }

    @Test
    void evaluationsAreListedWithTheirSpecializationAndPagination() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        long activity = post(t, "/api/v1/activities", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "Taller", "maximumScore", 5), 201).get("id").asLong();
        long session = post(t, "/api/v1/attendance-sessions", Map.of("teachingPeriodId", c.teachingPeriodId(), "sessionDate", YEAR + "-02-02"), 201).get("id").asLong();
        long exam = post(t, "/api/v1/exams", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "Parcial", "numberOfQuestions", 5), 201).get("id").asLong();

        JsonNode all = get(t, "/api/v1/evaluations?teachingPeriodId=" + c.teachingPeriodId() + "&size=2", 200);
        assertThat(all.get("totalElements").asInt()).isEqualTo(3);
        assertThat(all.get("content").size()).isEqualTo(2);
        JsonNode exams = get(t, "/api/v1/evaluations?teachingPeriodId=" + c.teachingPeriodId() + "&categoryId=" + categoryId("EXAMS"), 200);
        assertThat(exams.get("content").get(0).get("examId").asLong()).isEqualTo(exam);
        assertThat(exams.get("content").get(0).get("maximumScore").decimalValue()).isEqualByComparingTo("5"); // 1 punto por pregunta
        JsonNode acts = get(t, "/api/v1/evaluations?teachingPeriodId=" + c.teachingPeriodId() + "&categoryId=" + categoryId("ACTIVITIES"), 200);
        assertThat(acts.get("content").get(0).get("activityId").asLong()).isEqualTo(activity);
        JsonNode att = get(t, "/api/v1/evaluations?teachingPeriodId=" + c.teachingPeriodId() + "&categoryId=" + categoryId("ATTENDANCE"), 200);
        assertThat(att.get("content").get(0).get("attendanceSessionId").asLong()).isEqualTo(session);

        JsonNode audit = get(t, "/api/v1/audit-logs?action=CREATE&size=50", 200);
        assertThat(audit.get("content").toString()).contains("Activity").contains("AttendanceSession").contains("Exam");
    }
}
