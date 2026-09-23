package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.core.shared.domain.vo.TabularData;
import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** El Excel combinado de un teaching period (hojas Students/Grades/Attendance) exporta e importa en un solo archivo. */
class ImportExportTeachingPeriodIntegrationTest extends IntegrationTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private byte[] downloadFull(Teacher t, long teachingPeriodId) {
        MvcResult result = download(t, "/api/v1/exports/teaching-periods/" + teachingPeriodId + "/full");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return result.getResponse().getContentAsByteArray();
    }

    private JsonNode uploadFull(Teacher t, long teachingPeriodId, byte[] content, int status) {
        MvcResult result = upload(t, "/api/v1/imports/teaching-periods/" + teachingPeriodId, "file", "full.xlsx", XLSX,
                content, Map.of());
        return parse(result, status);
    }

    private String header(Sheet sheet, int column) {
        return sheet.getRow(0).getCell(column).getStringCellValue();
    }

    @Test
    void fullExportHasThreeSheetsAndRoundTripsGradesAttendanceAndNewStudents() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        List<Student> roster = importStudents(t, c, 2);
        Student s1 = roster.get(0);
        Student s2 = roster.get(1);

        long activityId = post(t, "/api/v1/activities", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "Taller 1",
                "maximumScore", 5), 201).get("id").asLong();
        long sessionId = post(t, "/api/v1/attendance-sessions", Map.of("teachingPeriodId", c.teachingPeriodId(),
                "sessionDate", YEAR + "-02-01", "maximumScore", 1), 201).get("id").asLong();

        byte[] initial = downloadFull(t, c.teachingPeriodId());
        Sheet students = sheet(initial, "Students");
        Sheet grades = sheet(initial, "Grades");
        Sheet attendance = sheet(initial, "Attendance");
        assertThat(students).isNotNull();
        assertThat(grades).isNotNull();
        assertThat(attendance).isNotNull();

        String gradeHeader = header(grades, 3);
        assertThat(gradeHeader).startsWith("Taller 1 #" + activityId).contains("max 5");
        String attendanceHeader = header(attendance, 3);
        assertThat(attendanceHeader).isEqualTo(YEAR + "-02-01 #" + sessionId);

        String newId = unique("ID");
        List<List<Object>> studentRows = new ArrayList<>(List.of(
                List.<Object>of(s1.identification(), "Nombre1", "Apellido1", ""),
                List.<Object>of(s2.identification(), "Nombre2", "Apellido2", ""),
                List.<Object>of(newId, "Nuevo", "Estudiante", "nuevo@example.com")));
        List<List<Object>> gradeRows = List.of(
                List.<Object>of(s1.identification(), "4"),
                List.<Object>of(s2.identification(), ""));
        List<List<Object>> attendanceRows = List.of(
                List.<Object>of(s1.identification(), "PRESENT"),
                List.<Object>of(s2.identification(), "ABSENT"));

        byte[] reupload = xlsxWorkbook(List.of(
                new TabularData("Students", List.of("identification_number", "first_name", "last_name", "email"), studentRows),
                new TabularData("Grades", List.of("identification_number", gradeHeader), gradeRows),
                new TabularData("Attendance", List.of("identification_number", attendanceHeader), attendanceRows)));

        JsonNode result = uploadFull(t, c.teachingPeriodId(), reupload, 201);
        assertThat(result.get("errors")).isEmpty();
        assertThat(result.get("failedRows").asInt()).isZero();
        assertThat(result.get("totalRows").asInt()).isEqualTo(7); // 3 students + 2 grades + 2 attendance
        assertThat(result.get("successfulRows").asInt()).isEqualTo(7);

        assertThat(get(t, "/api/v1/students?groupId=" + c.groupId() + "&size=100", 200).get("totalElements").asInt())
                .isEqualTo(3);

        JsonNode activityGrades = get(t, "/api/v1/activities/" + activityId + "/grades", 200);
        Map<Long, Double> gradeByStudent = new java.util.HashMap<>();
        activityGrades.forEach(g -> gradeByStudent.put(g.get("studentId").asLong(), g.get("grade").isNull() ? null : g.get("grade").asDouble()));
        assertThat(gradeByStudent.get(s1.id())).isEqualTo(4.0);
        assertThat(activityGrades.toString()).contains("\"studentId\":" + s2.id());

        JsonNode session = get(t, "/api/v1/attendance-sessions/" + sessionId, 200);
        JsonNode byStudent = session.get("students");
        String statusOf = null;
        for (JsonNode n : byStudent) {
            if (n.get("studentId").asLong() == s1.id()) {
                statusOf = n.get("status").asText();
            }
        }
        assertThat(statusOf).isEqualTo("PRESENT");
    }

    @Test
    void gradeOutOfRangeIsReportedWithoutBlockingOtherRows() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        List<Student> roster = importStudents(t, c, 2);
        Student s1 = roster.get(0);
        Student s2 = roster.get(1);
        long activityId = post(t, "/api/v1/activities", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "Quiz",
                "maximumScore", 5), 201).get("id").asLong();

        byte[] initial = downloadFull(t, c.teachingPeriodId());
        String gradeHeader = header(sheet(initial, "Grades"), 3);

        byte[] upload = xlsxWorkbook(List.of(new TabularData("Grades", List.of("identification_number", gradeHeader),
                List.of(List.<Object>of(s1.identification(), "10"), List.<Object>of(s2.identification(), "3")))));

        JsonNode result = uploadFull(t, c.teachingPeriodId(), upload, 201);
        assertThat(result.get("failedRows").asInt()).isEqualTo(1);
        assertThat(result.get("successfulRows").asInt()).isEqualTo(1);
        assertThat(result.get("errors").toString()).contains("between 0 and 5");

        JsonNode activityGrades = get(t, "/api/v1/activities/" + activityId + "/grades", 200);
        for (JsonNode g : activityGrades) {
            if (g.get("studentId").asLong() == s2.id()) {
                assertThat(g.get("grade").asDouble()).isEqualTo(3.0);
            }
            if (g.get("studentId").asLong() == s1.id()) {
                assertThat(g.get("grade").isNull()).isTrue();
            }
        }
    }

    @Test
    void allSheetsAreOptional() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        byte[] empty = xlsxWorkbook(List.of(new TabularData("Notes", List.of("anything"), List.of())));
        JsonNode result = uploadFull(t, c.teachingPeriodId(), empty, 201);
        assertThat(result.get("totalRows").asInt()).isZero();
        assertThat(result.get("status").asText()).isEqualTo("COMPLETED");
    }
}
