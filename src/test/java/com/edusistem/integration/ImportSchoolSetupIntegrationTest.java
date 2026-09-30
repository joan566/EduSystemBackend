package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.core.shared.domain.vo.TabularData;
import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Import combinado de la configuración completa de un profesor (10 hojas: AcademicPeriods, AcademicGrades, Subjects,
 * Groups, Classes —con su configuración de notas—, Schedules, Students, Activities, ActivityGrades, Attendance) desde
 * cero, sin nada pre-creado, y el export con el mismo formato.
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
    void templateHasAllSheets() {
        MvcResult result = download(newTeacher(), "/api/v1/imports/school-setup/template");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("Content-Disposition")).contains("plantilla-configuracion-escolar.xlsx");
        byte[] content = result.getResponse().getContentAsByteArray();
        for (String name : List.of("Instrucciones", "Periodos", "Grados", "Asignaturas", "Grupos", "Clases", "Horarios",
                "Estudiantes", "Actividades", "Notas de actividades", "Asistencia")) {
            assertThat(sheet(content, name)).as(name).isNotNull();
        }
        assertThat(sheet(content, "Asistencia").getRow(1).getCell(7).getStringCellValue()).isEqualTo("Presente");
        Sheet classes = sheet(content, "Clases");
        assertThat(classes.getRow(0).getCell(7).getStringCellValue()).isEqualTo("% Exámenes");
        assertThat(classes.getRow(0).getCell(7).getCellComment()).isNotNull();
        assertThat(classes.getDataValidations()).isNotEmpty();
        assertThat(sheet(content, "Horarios").getRow(1).getCell(5).getStringCellValue()).isEqualTo("Lunes");
    }

    /** La plantilla en español, tal como se descarga, se puede volver a subir y crea todo sin errores. */
    @Test
    void templateCanBeUploadedAsIs() {
        Teacher t = newTeacher();
        byte[] template = download(t, "/api/v1/imports/school-setup/template").getResponse().getContentAsByteArray();
        JsonNode result = uploadSchoolSetup(t, template, 201);
        assertThat(result.get("errors")).isEmpty();
        assertThat(result.get("failedRows").asInt()).isZero();
        assertThat(result.get("totalRows").asInt()).isEqualTo(10);
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
        assertThat(result.get("errors").toString()).contains("Grupos:Grado").contains("no existe");

        long gradeId = findByName(t, "/api/v1/grades", "name", gradeName);
        JsonNode groups = get(t, "/api/v1/groups?gradeId=" + gradeId + "&academicYear=" + YEAR, 200).get("content");
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).get("name").asText()).isEqualTo("A");
    }

    // ------------------------------------------------------------------ configuración de notas y horarios

    private static final List<String> CLASS_HEADERS = List.of("Grado", "Grupo", "Año lectivo", "Asignatura", "Periodo");

    private record Setup(String gradeName, String groupName, String subjectName, String periodName) {

        List<Object> classKey() {
            return List.of(gradeName, groupName, YEAR, subjectName, periodName);
        }
    }

    private static List<String> headers(List<String> base, String... more) {
        List<String> all = new ArrayList<>(base);
        all.addAll(List.of(more));
        return all;
    }

    private static List<Object> row(List<Object> base, Object... more) {
        List<Object> all = new ArrayList<>(base);
        all.addAll(List.of(more));
        return all;
    }

    /** Estructura mínima (periodo, grado, asignatura, grupo) escrita como la escribiría un profesor, en español. */
    private List<TabularData> structure(Setup s) {
        return List.of(
                new TabularData("Periodos", List.of("Nombre", "Fecha de inicio", "Fecha de fin"),
                        List.of(List.<Object>of(s.periodName(), "15/01/" + YEAR, "30/06/" + YEAR))),
                new TabularData("Grados", List.of("Nombre"), List.of(List.<Object>of(s.gradeName()))),
                new TabularData("Asignaturas", List.of("Nombre"), List.of(List.<Object>of(s.subjectName()))),
                new TabularData("Grupos", List.of("Grado", "Nombre", "Año lectivo"),
                        List.of(List.<Object>of(s.gradeName(), s.groupName(), YEAR))));
    }

    private static TabularData classes(Setup s, Object scale, Object passing, Object exams, Object activities,
                                       Object attendance) {
        return new TabularData("Clases", headers(CLASS_HEADERS, "Escala", "Nota mínima", "% Exámenes", "% Actividades",
                "% Asistencia"), List.of(row(s.classKey(), scale, passing, exams, activities, attendance)));
    }

    private static TabularData schedules(Setup s, List<List<Object>> blocks) {
        return new TabularData("Horarios", headers(CLASS_HEADERS, "Día", "Hora de inicio", "Hora de fin", "Salón"),
                blocks.stream().map(b -> row(s.classKey(), b.toArray())).toList());
    }

    private static List<TabularData> concat(List<TabularData> base, TabularData... more) {
        List<TabularData> all = new ArrayList<>(base);
        all.addAll(List.of(more));
        return all;
    }

    private long teachingPeriodId(Teacher t, Setup s) {
        long periodId = findByName(t, "/api/v1/academic-periods?size=100", "name", s.periodName());
        long gradeId = findByName(t, "/api/v1/grades", "name", s.gradeName());
        long subjectId = findByName(t, "/api/v1/subjects?name=" + s.subjectName(), "name", s.subjectName());
        long groupId = findByName(t, "/api/v1/groups?gradeId=" + gradeId + "&academicYear=" + YEAR, "name",
                s.groupName());
        long assignmentId = findByName(t, "/api/v1/teaching-assignments?groupId=" + groupId + "&subjectId=" + subjectId,
                "groupName", s.groupName());
        return findByName(t, "/api/v1/teaching-periods?teachingAssignmentId=" + assignmentId + "&academicPeriodId="
                + periodId, "groupName", s.groupName());
    }

    private Setup newSetup() {
        return new Setup(unique("Grade"), unique("Group"), unique("Subject"), unique("Period"));
    }

    @Test
    void classesSheetConfiguresGradingAndSchedulesSheetCreatesBlocks() {
        Teacher t = newTeacher();
        Setup s = newSetup();
        JsonNode result = uploadSchoolSetup(t, xlsxWorkbook(concat(structure(s),
                classes(s, "0 a 10", "6", "50%", 30, "20"),
                schedules(s, List.of(List.of("Lunes", "7:00", "08:00", "Salón 1"),
                        List.of("miercoles", "2:00 p. m.", "15:00", ""))))), 201);
        assertThat(result.get("errors")).isEmpty();
        assertThat(result.get("failedRows").asInt()).isZero();

        long tp = teachingPeriodId(t, s);
        JsonNode config = get(t, "/api/v1/teaching-periods/" + tp + "/grading-configuration", 200);
        assertThat(config.get("scale").get("maximumValue").asDouble()).isEqualTo(10);
        assertThat(config.get("passingGrade").asDouble()).isEqualTo(6);
        assertThat(config.get("totalWeight").asDouble()).isEqualTo(100);
        assertThat(config.get("complete").asBoolean()).isTrue();

        JsonNode blocks = get(t, "/api/v1/teaching-periods/" + tp + "/schedules", 200);
        assertThat(blocks).hasSize(2);
        assertThat(blocks.get(0).get("dayOfWeek").asText()).isEqualTo("MONDAY");
        assertThat(blocks.get(0).get("room").asText()).isEqualTo("Salón 1");
        assertThat(blocks.get(1).get("dayOfWeek").asText()).isEqualTo("WEDNESDAY");
        assertThat(blocks.get(1).get("startTime").asText()).isEqualTo("14:00");

        // segunda subida: nuevos pesos sin escala (se conserva 0-10), mismos horarios (no se duplican)
        result = uploadSchoolSetup(t, xlsxWorkbook(List.of(
                classes(s, "", "", 40, 40, 20),
                schedules(s, List.of(List.of("Lunes", "07:00", "08:00", "Salón 1"))))), 201);
        assertThat(result.get("errors")).isEmpty();
        config = get(t, "/api/v1/teaching-periods/" + tp + "/grading-configuration", 200);
        assertThat(config.get("scale").get("maximumValue").asDouble()).isEqualTo(10);
        assertThat(config.get("passingGrade").asDouble()).isEqualTo(6);
        assertThat(config.get("weights").toString()).contains("40");
        assertThat(get(t, "/api/v1/teaching-periods/" + tp + "/schedules", 200)).hasSize(2);

        // celdas de configuración vacías: no se toca nada
        uploadSchoolSetup(t, xlsxWorkbook(List.of(classes(s, "", "", "", "", ""))), 201);
        assertThat(get(t, "/api/v1/teaching-periods/" + tp + "/grading-configuration", 200).get("weights").toString())
                .contains("40");
    }

    @Test
    void invalidGradingAndScheduleCellsAreReportedInSpanishWithoutBlockingOtherRows() {
        Teacher t = newTeacher();
        Setup s = newSetup();
        Setup other = new Setup(s.gradeName(), s.groupName(), unique("Subject"), s.periodName());
        Setup invalid = new Setup(s.gradeName(), s.groupName(), unique("Subject"), s.periodName());
        List<TabularData> sheets = new ArrayList<>(structure(s));
        sheets.set(2, new TabularData("Asignaturas", List.of("Nombre"), List.of(List.<Object>of(s.subjectName()),
                List.<Object>of(other.subjectName()), List.<Object>of(invalid.subjectName()))));
        sheets.add(new TabularData("Clases", headers(CLASS_HEADERS, "Escala", "% Exámenes", "% Actividades",
                "% Asistencia"), List.of(
                        row(s.classKey(), "0-5", 40, 40, 20),
                        row(other.classKey(), "", "", "", ""),
                        row(invalid.classKey(), "0-7", 60, 40, 20))));
        sheets.add(new TabularData("Horarios", headers(CLASS_HEADERS, "Día", "Hora de inicio", "Hora de fin"), List.of(
                row(s.classKey(), "Lunes", "07:00", "08:00"),
                row(other.classKey(), "Lunes", "07:30", "08:30"),
                row(s.classKey(), "Martes", "09:00", "08:00"),
                row(s.classKey(), "Feriado", "09:00", "10:00"))));

        JsonNode result = uploadSchoolSetup(t, xlsxWorkbook(sheets), 201);
        String errors = result.get("errors").toString();
        assertThat(errors).contains("Clases:% Exámenes").contains("suman 120");
        assertThat(errors).contains("Clases:Escala").contains("Escala desconocida");
        assertThat(errors).contains("Se cruza con la clase " + s.subjectName());
        assertThat(errors).contains("Horarios:Hora de fin").contains("posterior a la hora de inicio");
        assertThat(errors).contains("Horarios:Día");
        assertThat(result.get("failedRows").asInt()).isEqualTo(4);

        long tp = teachingPeriodId(t, s);
        assertThat(get(t, "/api/v1/teaching-periods/" + tp + "/grading-configuration", 200).get("complete").asBoolean())
                .isTrue();
        assertThat(get(t, "/api/v1/teaching-periods/" + tp + "/schedules", 200)).hasSize(1);
        get(t, "/api/v1/teaching-periods/" + teachingPeriodId(t, other) + "/grading-configuration", 404);
    }

    @Test
    void oldNineSheetTemplateWithoutNewColumnsStillImports() {
        Teacher t = newTeacher();
        Setup s = newSetup();
        JsonNode result = uploadSchoolSetup(t, xlsxWorkbook(concat(structure(s),
                new TabularData("Classes", List.of("grade_name", "group_name", "academic_year", "subject_name",
                        "academic_period_name"), List.of(s.classKey())))), 201);
        assertThat(result.get("errors")).isEmpty();
        get(t, "/api/v1/teaching-periods/" + teachingPeriodId(t, s) + "/grading-configuration", 404);
    }

    // ------------------------------------------------------------------ export en el mismo formato

    @Test
    void exportContainsOnlyOwnDataAndCanBeUploadedAgainWithoutDuplicates() {
        Teacher t = newTeacher();
        byte[] template = download(t, "/api/v1/imports/school-setup/template").getResponse().getContentAsByteArray();
        assertThat(uploadSchoolSetup(t, template, 201).get("errors")).isEmpty();

        MvcResult export = download(t, "/api/v1/exports/school-setup");
        assertThat(export.getResponse().getStatus()).isEqualTo(200);
        assertThat(export.getResponse().getHeader("Content-Disposition")).contains("configuracion-escolar.xlsx");
        byte[] content = export.getResponse().getContentAsByteArray();

        Row classRow = sheet(content, "Clases").getRow(1);
        assertThat(classRow.getCell(5).getStringCellValue()).isEqualTo("0-5");
        assertThat(classRow.getCell(6).getNumericCellValue()).isEqualTo(3);
        assertThat(classRow.getCell(7).getNumericCellValue()).isEqualTo(40);
        assertThat(classRow.getCell(9).getNumericCellValue()).isEqualTo(20);
        assertThat(sheet(content, "Horarios").getRow(1).getCell(5).getStringCellValue()).isEqualTo("Lunes");
        assertThat(sheet(content, "Estudiantes").getLastRowNum()).isEqualTo(1);
        assertThat(sheet(content, "Notas de actividades").getRow(1).getCell(7).getNumericCellValue()).isEqualTo(4.5);
        assertThat(sheet(content, "Asistencia").getRow(1).getCell(7).getStringCellValue()).isEqualTo("Presente");

        byte[] otherExport = download(newTeacher(), "/api/v1/exports/school-setup").getResponse().getContentAsByteArray();
        for (String name : List.of("Periodos", "Clases", "Horarios", "Estudiantes", "Asistencia")) {
            assertThat(sheet(otherExport, name).getLastRowNum()).as(name).isZero();
        }

        JsonNode reupload = uploadSchoolSetup(t, content, 201);
        assertThat(reupload.get("errors")).isEmpty();
        assertThat(reupload.get("failedRows").asInt()).isZero();
        MvcResult again = download(t, "/api/v1/exports/school-setup");
        byte[] againContent = again.getResponse().getContentAsByteArray();
        for (String name : List.of("Periodos", "Clases", "Horarios", "Estudiantes", "Actividades", "Notas de actividades",
                "Asistencia")) {
            assertThat(sheet(againContent, name).getLastRowNum()).as(name)
                    .isEqualTo(sheet(content, name).getLastRowNum());
        }
    }
}
