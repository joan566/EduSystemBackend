package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class ExportIntegrationTest extends IntegrationTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private Sheet xlsx(MvcResult result) {
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).isEqualTo(XLSX);
        assertThat(result.getResponse().getHeader("Content-Disposition")).contains(".xlsx");
        return firstSheet(result.getResponse().getContentAsByteArray());
    }

    private List<String> headers(Sheet sheet) {
        List<String> headers = new ArrayList<>();
        sheet.getRow(0).forEach(c -> headers.add(c.getStringCellValue()));
        return headers;
    }

    @Test
    void exportsStudentsFilteredByTeachingPeriodGroupOrAll() {
        Teacher t = newTeacher();
        Context c1 = newContext(t);
        Context c2 = newContext(t);
        importStudents(t, c1, 3);
        importStudents(t, c2, 2);

        Sheet byPeriod = xlsx(download(t, "/api/v1/exports/students?teachingPeriodId=" + c1.teachingPeriodId()));
        assertThat(byPeriod.getLastRowNum()).isEqualTo(3);
        assertThat(headers(byPeriod)).containsExactly("student_code", "identification_number", "first_name", "last_name", "email");
        assertThat(xlsx(download(t, "/api/v1/exports/students?groupId=" + c2.groupId())).getLastRowNum()).isEqualTo(2);
        assertThat(xlsx(download(t, "/api/v1/exports/students")).getLastRowNum()).isEqualTo(5);
        assertThat(get(t, "/api/v1/audit-logs?action=EXPORT", 200).get("totalElements").asInt()).isEqualTo(3);
    }

    @Test
    void exportsGradesWithPeriodGradeWhenTheConfigurationIsComplete() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        List<Student> students = importStudents(t, c, 2);
        long activity = post(t, "/api/v1/activities", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "Taller 1",
                "maximumScore", 5), 201).get("id").asLong();
        put(t, "/api/v1/activities/" + activity + "/grades", Map.of("grades", List.of(
                Map.of("studentId", students.get(0).id(), "grade", 4.5))), 200);

        Sheet sheet = xlsx(download(t, "/api/v1/exports/grades?teachingPeriodId=" + c.teachingPeriodId()));
        List<String> headers = headers(sheet);
        assertThat(headers).contains("student_code", "Taller 1 (max 5)", "period_grade");
        assertThat(sheet.getLastRowNum()).isEqualTo(2);
        int gradeColumn = headers.indexOf("Taller 1 (max 5)");
        int codeColumn = headers.indexOf("student_code");
        for (int r = 1; r <= 2; r++) {
            String code = sheet.getRow(r).getCell(codeColumn).getStringCellValue();
            var cell = sheet.getRow(r).getCell(gradeColumn);
            if (code.equals(students.get(0).code())) {
                assertThat(cell.getNumericCellValue()).isEqualTo(4.5);
            } else {
                assertThat(cell == null || cell.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK).isTrue();
            }
        }

        // con pesos parciales no se calcula la nota del periodo, pero el resto se exporta
        put(t, "/api/v1/teaching-periods/" + c.teachingPeriodId() + "/grading-configuration", Map.of("gradingScaleId", scaleId("Colombian 0-5"),
                "weights", List.of(Map.of("evaluationCategoryId", categoryId("EXAMS"), "weight", 50))), 200);
        assertThat(headers(xlsx(download(t, "/api/v1/exports/grades?teachingPeriodId=" + c.teachingPeriodId())))).doesNotContain("period_grade");
    }

    @Test
    void exportsAttendanceMatrixWithTotals() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        Student s = importStudents(t, c, 1).get(0);
        String[] statuses = {"PRESENT", "ABSENT", "EXCUSED", "PRESENT"};
        for (int i = 0; i < statuses.length; i++) {
            long session = post(t, "/api/v1/attendance-sessions", Map.of("teachingPeriodId", c.teachingPeriodId(),
                    "sessionDate", YEAR + "-02-0" + (i + 1)), 201).get("id").asLong();
            put(t, "/api/v1/attendance-sessions/" + session + "/records", Map.of("records", List.of(
                    Map.of("studentId", s.id(), "status", statuses[i]))), 200);
        }
        Sheet sheet = xlsx(download(t, "/api/v1/exports/attendance?teachingPeriodId=" + c.teachingPeriodId()));
        List<String> headers = headers(sheet);
        assertThat(headers).contains(YEAR + "-02-01", YEAR + "-02-04", "present", "absent", "excused", "attendance_percent");
        var row = sheet.getRow(1);
        assertThat(row.getCell(headers.indexOf(YEAR + "-02-01")).getStringCellValue()).isEqualTo("PRESENT");
        assertThat(row.getCell(headers.indexOf(YEAR + "-02-03")).getStringCellValue()).isEqualTo("EXCUSED");
        assertThat(row.getCell(headers.indexOf("present")).getNumericCellValue()).isEqualTo(2);
        assertThat(row.getCell(headers.indexOf("absent")).getNumericCellValue()).isEqualTo(1);
        assertThat(row.getCell(headers.indexOf("attendance_percent")).getNumericCellValue()).isEqualTo(66.7); // excusa no cuenta
    }
}
