package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuthorizationIntegrationTest extends IntegrationTest {

    private long createExam(Teacher t, Context c) {
        return post(t, "/api/v1/exams", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "Parcial",
                "numberOfQuestions", 2, "questions", List.of(
                        Map.of("questionNumber", 1, "statement", "2+2?", "correctOption", "B", "options", List.of(
                                Map.of("letter", "A", "text", "3"), Map.of("letter", "B", "text", "4"))),
                        Map.of("questionNumber", 2, "statement", "1+1?", "correctOption", "A", "options", List.of(
                                Map.of("letter", "A", "text", "2"), Map.of("letter", "B", "text", "3"))))), 201)
                .get("id").asLong();
    }

    @Test
    void everyBusinessEndpointRequiresAuthentication() {
        for (String url : new String[]{"/api/v1/students", "/api/v1/grades", "/api/v1/groups", "/api/v1/subjects",
                "/api/v1/academic-periods", "/api/v1/teaching-assignments", "/api/v1/teaching-periods",
                "/api/v1/evaluations?teachingPeriodId=1", "/api/v1/exams?teachingPeriodId=1", "/api/v1/imports",
                "/api/v1/exports/students", "/api/v1/audit-logs", "/api/v1/users/me", "/api/v1/grading-scales"}) {
            assertError(get(null, url, 401), 401, "UNAUTHORIZED");
        }
        assertError(post(null, "/api/v1/grades", Map.of("name", "X"), 401), 401, "UNAUTHORIZED");
    }

    @Test
    void tokenSignedWithAnotherKeyOrExpiredIsRejected() {
        Teacher t = newTeacher();
        String forged = Jwts.builder().subject(String.valueOf(t.id())).claim("roles", List.of("TEACHER"))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("another-secret-another-secret-another-1234".getBytes(StandardCharsets.UTF_8))).compact();
        assertError(get(new Teacher(t.id(), t.email(), forged), "/api/v1/auth/me", 401), 401, "UNAUTHORIZED");
        String expired = Jwts.builder().subject(String.valueOf(t.id())).claim("roles", List.of("TEACHER"))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor("test-secret-test-secret-test-secret-1234".getBytes(StandardCharsets.UTF_8))).compact();
        assertError(get(new Teacher(t.id(), t.email(), expired), "/api/v1/auth/me", 401), 401, "UNAUTHORIZED");
    }

    @Test
    void teacherAccessesOwnDataAndCannotSeeAnotherTeachersData() {
        Teacher a = newTeacher();
        Teacher b = newTeacher();
        Context ca = newContext(a);
        importStudents(a, ca, 2);
        long examId = createExam(a, ca);

        // el dueño accede
        assertThat(get(a, "/api/v1/exams/" + examId, 200).get("id").asLong()).isEqualTo(examId);
        assertThat(get(a, "/api/v1/teaching-periods/" + ca.teachingPeriodId(), 200).get("id").asLong()).isEqualTo(ca.teachingPeriodId());
        assertThat(get(a, "/api/v1/students?groupId=" + ca.groupId(), 200).get("totalElements").asInt()).isEqualTo(2);

        // el otro profesor recibe 404 (no se revela la existencia) en todo lo que pertenece a A
        assertError(get(b, "/api/v1/exams/" + examId, 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(b, "/api/v1/teaching-periods/" + ca.teachingPeriodId(), 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(b, "/api/v1/teaching-assignments/" + ca.assignmentId(), 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(b, "/api/v1/exams?teachingPeriodId=" + ca.teachingPeriodId(), 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(b, "/api/v1/evaluations?teachingPeriodId=" + ca.teachingPeriodId(), 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(b, "/api/v1/teaching-periods/" + ca.teachingPeriodId() + "/grading-configuration", 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(b, "/api/v1/teaching-periods/" + ca.teachingPeriodId() + "/period-grades", 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(b, "/api/v1/exports/grades?teachingPeriodId=" + ca.teachingPeriodId(), 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(b, "/api/v1/exports/attendance?teachingPeriodId=" + ca.teachingPeriodId(), 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(b, "/api/v1/exports/students?groupId=" + ca.groupId(), 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(b, "/api/v1/students?groupId=" + ca.groupId(), 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(b, "/api/v1/exams/" + examId + "/answer-sheets", 404), 404, "RESOURCE_NOT_FOUND");
        assertError(put(b, "/api/v1/exams/" + examId, Map.of("name", "Hackeado"), 404), 404, "RESOURCE_NOT_FOUND");
        assertError(call("DELETE", b, "/api/v1/exams/" + examId, null, 404), 404, "RESOURCE_NOT_FOUND");
        assertError(put(b, "/api/v1/teaching-periods/" + ca.teachingPeriodId() + "/grading-configuration",
                Map.of("gradingScaleId", scaleId("Scale 0-10"), "weights", List.of(Map.of("evaluationCategoryId", categoryId("EXAMS"), "weight", 100))), 404),
                404, "RESOURCE_NOT_FOUND");
        assertError(post(b, "/api/v1/teaching-periods", Map.of("teachingAssignmentId", ca.assignmentId(),
                "academicPeriodId", ca.academicPeriodId()), 404), 404, "RESOURCE_NOT_FOUND");

        // los estudiantes de A no aparecen para B
        Context cb = newContext(b);
        assertThat(get(b, "/api/v1/students", 200).get("totalElements").asInt()).isZero();
        long studentOfA = listStudents(a, ca.groupId()).get(0).id();
        assertError(get(b, "/api/v1/students/" + studentOfA, 404), 404, "RESOURCE_NOT_FOUND");
        assertThat(get(b, "/api/v1/teaching-assignments", 200).get("content").size()).isEqualTo(1);
        assertThat(cb.groupId()).isNotEqualTo(ca.groupId());

        // y nada de esto modificó los datos de A
        assertThat(get(a, "/api/v1/exams/" + examId, 200).get("name").asText()).isEqualTo("Parcial");
    }

    @Test
    void sharedCatalogItemsUsedByAnotherTeacherCannotBeModifiedOrDeleted() {
        Teacher a = newTeacher();
        Teacher b = newTeacher();
        Context ca = newContext(a);
        assertError(put(b, "/api/v1/groups/" + ca.groupId(), Map.of("name", "Renombrado", "academicYear", YEAR), 409), 409, "CATALOG_ITEM_IN_USE");
        assertError(put(b, "/api/v1/grades/" + ca.gradeId(), Map.of("name", "Otro"), 409), 409, "CATALOG_ITEM_IN_USE");
        assertError(put(b, "/api/v1/subjects/" + ca.subjectId(), Map.of("name", "Otro"), 409), 409, "CATALOG_ITEM_IN_USE");
        assertError(put(b, "/api/v1/academic-periods/" + ca.academicPeriodId(),
                Map.of("name", "X", "startDate", YEAR + "-01-01", "endDate", YEAR + "-02-01"), 409), 409, "CATALOG_ITEM_IN_USE");
        assertError(call("DELETE", b, "/api/v1/grades/" + ca.gradeId(), null, 409), 409, "GRADE_HAS_GROUPS");
        assertError(call("DELETE", b, "/api/v1/groups/" + ca.groupId(), null, 409), 409, "GROUP_HAS_DEPENDENTS");
        assertError(call("DELETE", b, "/api/v1/subjects/" + ca.subjectId(), null, 409), 409, "SUBJECT_HAS_TEACHING_ASSIGNMENTS");
        // el mismo grupo compartido sí puede asignarse a otro profesor (escenario Joan/Carlos)
        JsonNode assignment = post(b, "/api/v1/teaching-assignments", Map.of("groupId", ca.groupId(), "subjectId", ca.subjectId()), 201);
        assertThat(assignment.get("groupId").asLong()).isEqualTo(ca.groupId());
    }

    @Test
    void teachingAssignmentCannotBeDuplicatedWhileActiveButCanBeReactivated() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        assertError(post(t, "/api/v1/teaching-assignments", Map.of("groupId", c.groupId(), "subjectId", c.subjectId()), 409),
                409, "TEACHING_ASSIGNMENT_ALREADY_EXISTS");
        call("PATCH", t, "/api/v1/teaching-assignments/" + c.assignmentId() + "/active", Map.of("active", false), 200);
        JsonNode reactivated = post(t, "/api/v1/teaching-assignments", Map.of("groupId", c.groupId(), "subjectId", c.subjectId()), 201);
        assertThat(reactivated.get("id").asLong()).isEqualTo(c.assignmentId());
        assertThat(reactivated.get("active").asBoolean()).isTrue();
    }

    @Test
    void gradeWithGroupsCannotBeDeletedAndUnusedOneCan() {
        Teacher t = newTeacher();
        long unused = post(t, "/api/v1/grades", Map.of("name", unique("Solo")), 201).get("id").asLong();
        call("DELETE", t, "/api/v1/grades/" + unused, null, 204);
        Context c = newContext(t);
        assertError(call("DELETE", t, "/api/v1/grades/" + c.gradeId(), null, 409), 409, "GRADE_HAS_GROUPS");
    }
}
