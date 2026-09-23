package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.core.shared.domain.vo.TabularData;
import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Import combinado de la configuración completa de un profesor (9 hojas: AcademicPeriods, AcademicGrades, Subjects,
 * Groups, Classes, Students, Activities, ActivityGrades, Attendance) desde cero, sin nada pre-creado.
 */
class ImportSchoolSetupIntegrationTest extends IntegrationTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private JsonNode uploadSchoolSetup(Teacher t, byte[] content, int status) {
        MvcResult result = upload(t, "/api/v1/imports/school-setup", "file", "school-setup.xlsx", XLSX, content, Map.of());
        return parse(result, status);
    }

    private long findByName(Teacher t, String url, String field, String name) {
        JsonNode body = get(t, url, 200);
        JsonNode items = body.has("content") ? body.get("content") : body;
        for (JsonNode n : items) {
            if (n.get(field).asText().equals(name)) {
                return n.get("id").asLong();
            }
        }
        throw new AssertionError("Not found: '" + name + "' in response of " + url + ": " + body);
    }

    @Test
    void templateHasNineSheets() {
        MvcResult result = download(newTeacher(), "/api/v1/imports/school-setup/template");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        byte[] content = result.getResponse().getContentAsByteArray();
        for (String name : List.of("AcademicPeriods", "AcademicGrades", "Subjects", "Groups", "Classes", "Students",
                "Activities", "ActivityGrades", "Attendance")) {
            assertThat(sheet(content, name)).as(name).isNotNull();
        }
    }

    @Test
    void schoolSetupCreatesEntireStructureFromScratch() {
        Teacher t = newTeacher();
        String periodName = unique("Period");
        String gradeName = unique("Grade");
        String subjectName = unique("Subject");
        String groupName = unique("Group");
        String studentId = unique("ID");

        byte[] workbook = xlsxWorkbook(List.of(
                new TabularData("AcademicPeriods", List.of("name", "start_date", "end_date"),
                        List.of(List.<Object>of(periodName, LocalDate.of(YEAR, 1, 15), LocalDate.of(YEAR, 6, 1)))),
                new TabularData("AcademicGrades", List.of("name", "description"),
                        List.of(List.<Object>of(gradeName, "Descripción"))),
                new TabularData("Subjects", List.of("name", "description"),
                        List.of(List.<Object>of(subjectName, "Descripción"))),
                new TabularData("Groups", List.of("grade_name", "name", "academic_year"),
                        List.of(List.<Object>of(gradeName, groupName, YEAR))),
                new TabularData("Classes",
                        List.of("grade_name", "group_name", "academic_year", "subject_name", "academic_period_name"),
                        List.of(List.<Object>of(gradeName, groupName, YEAR, subjectName, periodName))),
                new TabularData("Students",
                        List.of("identification_number", "first_name", "last_name", "email", "grade_name", "group_name",
                                "academic_year"),
                        List.of(List.<Object>of(studentId, "Nombre", "Apellido", "", gradeName, groupName, YEAR))),
                new TabularData("Activities",
                        List.of("grade_name", "group_name", "academic_year", "subject_name", "academic_period_name",
                                "name", "maximum_score"),
                        List.of(List.<Object>of(gradeName, groupName, YEAR, subjectName, periodName, "Taller 1",
                                BigDecimal.valueOf(5)))),
                new TabularData("ActivityGrades",
                        List.of("grade_name", "group_name", "academic_year", "subject_name", "academic_period_name",
                                "activity_name", "identification_number", "grade"),
                        List.of(List.<Object>of(gradeName, groupName, YEAR, subjectName, periodName, "Taller 1", studentId,
                                BigDecimal.valueOf(4.5)))),
                new TabularData("Attendance",
                        List.of("grade_name", "group_name", "academic_year", "subject_name", "academic_period_name",
                                "session_date", "identification_number", "status"),
                        List.of(List.<Object>of(gradeName, groupName, YEAR, subjectName, periodName,
                                LocalDate.of(YEAR, 2, 1), studentId, "PRESENT")))));

        JsonNode result = uploadSchoolSetup(t, workbook, 201);
        assertThat(result.get("errors")).isEmpty();
        assertThat(result.get("failedRows").asInt()).isZero();
        assertThat(result.get("totalRows").asInt()).isEqualTo(9);
        assertThat(result.get("successfulRows").asInt()).isEqualTo(9);

        long periodId = findByName(t, "/api/v1/academic-periods?size=100", "name", periodName);
        long gradeId = findByName(t, "/api/v1/grades", "name", gradeName);
        long subjectId = findByName(t, "/api/v1/subjects?name=" + subjectName, "name", subjectName);
        long groupId = findByName(t, "/api/v1/groups?gradeId=" + gradeId + "&academicYear=" + YEAR, "name", groupName);
        long assignmentId = findByName(t, "/api/v1/teaching-assignments?groupId=" + groupId + "&subjectId=" + subjectId,
                "groupName", groupName);
        long teachingPeriodId = findByName(t, "/api/v1/teaching-periods?teachingAssignmentId=" + assignmentId
                + "&academicPeriodId=" + periodId, "groupName", groupName);

        JsonNode students = get(t, "/api/v1/students?groupId=" + groupId + "&size=100", 200).get("content");
        assertThat(students).hasSize(1);
        long studentDbId = students.get(0).get("id").asLong();
        assertThat(students.get(0).get("identificationNumber").asText()).isEqualTo(studentId);

        JsonNode activities = get(t, "/api/v1/activities?teachingPeriodId=" + teachingPeriodId, 200).get("content");
        assertThat(activities).hasSize(1);
        assertThat(activities.get(0).get("name").asText()).isEqualTo("Taller 1");
        long activityId = activities.get(0).get("id").asLong();

        JsonNode grades = get(t, "/api/v1/activities/" + activityId + "/grades", 200);
        Double grade = null;
        for (JsonNode g : grades) {
            if (g.get("studentId").asLong() == studentDbId) {
                grade = g.get("grade").asDouble();
            }
        }
        assertThat(grade).isEqualTo(4.5);

        JsonNode sessions = get(t, "/api/v1/attendance-sessions?teachingPeriodId=" + teachingPeriodId, 200).get("content");
        assertThat(sessions).hasSize(1);
        long sessionId = sessions.get(0).get("id").asLong();
        JsonNode session = get(t, "/api/v1/attendance-sessions/" + sessionId, 200);
        String status = null;
        for (JsonNode n : session.get("students")) {
            if (n.get("studentId").asLong() == studentDbId) {
                status = n.get("status").asText();
            }
        }
        assertThat(status).isEqualTo("PRESENT");
    }

    @Test
    void invalidGroupRowIsReportedWithoutBlockingOtherRows() {
        Teacher t = newTeacher();
        String gradeName = unique("Grade");
        String missingGradeName = unique("MissingGrade");

        byte[] workbook = xlsxWorkbook(List.of(
                new TabularData("AcademicGrades", List.of("name"), List.of(List.<Object>of(gradeName))),
                new TabularData("Groups", List.of("grade_name", "name", "academic_year"), List.of(
                        List.<Object>of(gradeName, "A", YEAR),
                        List.<Object>of(missingGradeName, "B", YEAR)))));

        JsonNode result = uploadSchoolSetup(t, workbook, 201);
        assertThat(result.get("totalRows").asInt()).isEqualTo(3); // 1 AcademicGrades + 2 Groups
        assertThat(result.get("failedRows").asInt()).isEqualTo(1);
        assertThat(result.get("successfulRows").asInt()).isEqualTo(2);
        assertThat(result.get("errors").toString()).contains("does not exist");

        long gradeId = findByName(t, "/api/v1/grades", "name", gradeName);
        JsonNode groups = get(t, "/api/v1/groups?gradeId=" + gradeId + "&academicYear=" + YEAR, 200).get("content");
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).get("name").asText()).isEqualTo("A");
    }
}
