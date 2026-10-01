package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.core.imports.domain.inputports.ProcessImportBatchUseCase;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

class ImportIntegrationTest extends IntegrationTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private FileStoragePort storage;
    @Autowired
    private ProcessImportBatchUseCase importWorker;

    /** Con 202 espera a que termine la importación y devuelve {@code GET /imports/{id}}. */
    private JsonNode importFile(Teacher t, byte[] content, String name, int status) {
        if (status == 202) {
            return runImport(t, "/api/v1/imports/students", name, content);
        }
        return parse(upload(t, "/api/v1/imports/students", "file", name, XLSX, content, Map.of()), status);
    }

    private List<Object> row(String id, String first, String last, String email, Context c, Object year) {
        return new ArrayList<>(java.util.Arrays.asList(id, first, last, email, c.gradeName(), c.groupName(), year));
    }

    @Test
    void validExcelCreatesStudentsAndEnrollsThemInTheGroup() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        String id1 = unique("ID");
        String id2 = unique("ID");
        JsonNode result = importFile(t, xlsx(IMPORT_HEADERS, List.of(
                row(id1, "Ana", "Pérez", "ana@example.com", c, YEAR),
                row(id2, "Luis", "Gómez", "", c, YEAR))), "students.xlsx", 202);

        assertThat(result.get("totalRows").asInt()).isEqualTo(2);
        assertThat(result.get("successfulRows").asInt()).isEqualTo(2);
        assertThat(result.get("failedRows").asInt()).isZero();
        assertThat(result.get("errors")).isEmpty();
        assertThat(result.get("status").asText()).isEqualTo("COMPLETED");

        JsonNode students = get(t, "/api/v1/students?groupId=" + c.groupId(), 200);
        assertThat(students.get("totalElements").asInt()).isEqualTo(2);
        JsonNode first = students.get("content").get(0);
        assertThat(first.get("studentCode").asText()).startsWith("EST-");
        JsonNode current = first.get("currentEnrollment");
        assertThat(current.get("groupId").asLong()).isEqualTo(c.groupId());
        assertThat(current.get("active").asBoolean()).isTrue();
        assertThat(students.toString()).contains("Pérez").contains("Gómez");
        assertThat(jdbc.queryForObject("select count(*) from student_groups where group_id = ? and active", Integer.class, c.groupId()))
                .isEqualTo(2);

        // A withdrawn student keeps their course in the listing, marked inactive.
        long withdrawn = students.get("content").get(1).get("id").asLong();
        post(t, "/api/v1/students/" + withdrawn + "/groups/" + c.groupId() + "/withdrawal", Map.of(), 204);
        JsonNode afterWithdrawal = get(t, "/api/v1/students?groupId=" + c.groupId(), 200).get("content");
        assertThat(afterWithdrawal).filteredOn(s -> s.get("id").asLong() == withdrawn).singleElement()
                .satisfies(s -> assertThat(s.get("currentEnrollment").get("active").asBoolean()).isFalse());

        Map<String, Object> batch = jdbc.queryForMap("select * from import_batches where id = ?", result.get("id").asLong());
        assertThat(batch.get("status")).isEqualTo("COMPLETED");
        assertThat(batch.get("user_id")).isEqualTo(t.id());
        assertThat(batch.get("total_rows")).isEqualTo(2);
        assertThat(get(t, "/api/v1/audit-logs?action=IMPORT", 200).get("content").size()).isEqualTo(1);
    }

    @Test
    void missingRequiredColumnsMarkTheBatchFailedWithTheReason() {
        Teacher t = newTeacher();
        JsonNode result = importFile(t, xlsx(List.of("identification_number", "first_name", "last_name"),
                List.of(List.of("1", "A", "B"))), "students.xlsx", 202);
        assertThat(result.get("status").asText()).isEqualTo("FAILED");
        assertThat(result.get("type").asText()).isEqualTo("STUDENTS");
        assertThat(result.get("errorCode").asText()).isEqualTo("MISSING_COLUMNS");
        assertThat(result.get("errorMessage").asText()).contains("Grado").contains("Grupo");
        assertThat(result.get("completedAt").isNull()).isFalse();
        assertThat(get(t, "/api/v1/audit-logs?action=IMPORT", 200).get("content").size()).isEqualTo(1);
    }

    @Test
    void invalidRowsDoNotAbortTheImportAndAreReported() throws Exception {
        Teacher t = newTeacher();
        Context c = newContext(t);
        String dup = unique("ID");
        List<List<Object>> rows = new ArrayList<>();
        rows.add(row(unique("ID"), "Valida", "Fila", "ok@example.com", c, YEAR));                 // fila 2 OK
        rows.add(row(unique("ID"), "", "SinNombre", "", c, YEAR));                                // fila 3: first_name requerido
        rows.add(row(unique("ID"), "Mala", "Email", "no-es-email", c, YEAR));                     // fila 4: email inválido
        List<Object> unknownGrade = row(unique("ID"), "Grado", "Inexistente", "", c, YEAR);
        unknownGrade.set(4, "99°");
        rows.add(unknownGrade);                                                                   // fila 5: grado inexistente
        List<Object> unknownGroup = row(unique("ID"), "Grupo", "Inexistente", "", c, YEAR);
        unknownGroup.set(5, "ZZZ");
        rows.add(unknownGroup);                                                                   // fila 6: grupo inexistente
        rows.add(row(dup, "Primera", "Vez", "", c, YEAR));                                        // fila 7 OK
        rows.add(row(dup, "Segunda", "Vez", "", c, YEAR));                                        // fila 8: duplicada en el archivo
        rows.add(row(unique("ID"), "Año", "Malo", "", c, "abc"));                                 // fila 9: año inválido

        JsonNode result = importFile(t, xlsx(IMPORT_HEADERS, rows), "students.xlsx", 202);
        assertThat(result.get("totalRows").asInt()).isEqualTo(8);
        assertThat(result.get("successfulRows").asInt()).isEqualTo(2);
        assertThat(result.get("failedRows").asInt()).isEqualTo(6);
        assertThat(result.get("status").asText()).isEqualTo("COMPLETED_WITH_ERRORS");

        List<Integer> failedRowNumbers = new ArrayList<>();
        result.get("errors").forEach(e -> failedRowNumbers.add(e.get("row").asInt()));
        assertThat(failedRowNumbers).containsExactlyInAnyOrder(3, 4, 5, 6, 8, 9);
        String errors = result.get("errors").toString();
        assertThat(errors).contains("Nombres").contains("Correo electrónico").contains("El grado '99°' no existe")
                .contains("El grupo 'ZZZ'").contains("Duplicado en el archivo").contains("Año lectivo");

        assertThat(get(t, "/api/v1/students?groupId=" + c.groupId(), 200).get("totalElements").asInt()).isEqualTo(2);

        // reporte de errores descargable en Excel
        MvcResult report = download(t, "/api/v1/imports/" + result.get("id").asLong() + "/error-report");
        assertThat(report.getResponse().getStatus()).isEqualTo(200);
        assertThat(report.getResponse().getHeader("Content-Disposition")).contains("errores.xlsx");
        Sheet sheet = firstSheet(report.getResponse().getContentAsByteArray());
        assertThat(sheet.getSheetName()).isEqualTo("Errores");
        assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Fila");
        assertThat(sheet.getLastRowNum()).isEqualTo(6); // encabezado + 6 errores
    }

    @Test
    void rejectsNonExcelEmptyAndCorruptFiles() {
        Teacher t = newTeacher();
        assertError(importFile(t, "a,b,c".getBytes(StandardCharsets.UTF_8), "students.csv", 400), 400, "INVALID_FILE_TYPE");
        assertError(importFile(t, "no soy un excel".getBytes(StandardCharsets.UTF_8), "students.xlsx", 400), 400, "INVALID_FILE_TYPE");
        assertError(importFile(t, new byte[0], "students.xlsx", 400), 400, "EMPTY_FILE");
        // parece un .xlsx pero no se puede abrir: se descubre al procesarlo
        JsonNode corrupt = importFile(t, "PK\u0003\u0004garbage-garbage-garbage".getBytes(StandardCharsets.ISO_8859_1),
                "students.xlsx", 202);
        assertThat(corrupt.get("status").asText()).isEqualTo("FAILED");
        assertThat(corrupt.get("errorCode").asText()).isEqualTo("INVALID_EXCEL");
    }

    @Test
    void reimportingUpdatesExistingStudentsInsteadOfDuplicatingThem() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        String id = unique("ID");
        importFile(t, xlsx(IMPORT_HEADERS, List.of(row(id, "Nombre", "Viejo", "", c, YEAR))), "a.xlsx", 202);
        JsonNode second = importFile(t, xlsx(IMPORT_HEADERS, List.of(row(id, "Nombre", "Nuevo", "nuevo@example.com", c, YEAR))), "b.xlsx", 202);
        assertThat(second.get("successfulRows").asInt()).isEqualTo(1);

        assertThat(jdbc.queryForObject("select count(*) from students where identification_number = ?", Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select last_name from students where identification_number = ?", String.class, id)).isEqualTo("Nuevo");
        assertThat(jdbc.queryForObject("select email from students where identification_number = ?", String.class, id)).isEqualTo("nuevo@example.com");
        assertThat(get(t, "/api/v1/students?groupId=" + c.groupId(), 200).get("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    void suppliedStudentCodeIsUsedAndCannotBelongToAnotherStudent() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        String code = unique("COD");
        List<String> headers = new ArrayList<>(IMPORT_HEADERS);
        headers.add("student_code");
        List<Object> first = row(unique("ID"), "Con", "Codigo", "", c, YEAR);
        first.add(code);
        List<Object> second = row(unique("ID"), "Otro", "Codigo", "", c, YEAR);
        second.add(code);
        JsonNode result = importFile(t, xlsx(headers, List.of(first, second)), "students.xlsx", 202);
        assertThat(result.get("successfulRows").asInt()).isEqualTo(1);
        assertThat(result.get("errors").toString()).contains("Código");
        assertThat(jdbc.queryForObject("select count(*) from students where student_code = ?", Integer.class, code)).isEqualTo(1);
    }

    @Test
    void cannotImportIntoAnotherTeachersGroup() {
        Teacher a = newTeacher();
        Teacher b = newTeacher();
        Context ca = newContext(a);
        JsonNode result = importFile(b, xlsx(IMPORT_HEADERS, List.of(row(unique("ID"), "Intruso", "X", "", ca, YEAR))), "s.xlsx", 202);
        assertThat(result.get("successfulRows").asInt()).isZero();
        assertThat(result.get("status").asText()).isEqualTo("FAILED");
        // los grados y grupos de A no existen para B
        assertThat(result.get("errors").toString()).contains("no existe");
        assertThat(get(a, "/api/v1/students?groupId=" + ca.groupId(), 200).get("totalElements").asInt()).isZero();
    }

    @Test
    void sameIdentificationCreatesIndependentStudentsPerTeacher() {
        Teacher a = newTeacher();
        Teacher b = newTeacher();
        Context ca = newContext(a);
        Context cb = newContext(b);
        String id = unique("ID");
        importFile(a, xlsx(IMPORT_HEADERS, List.of(row(id, "Ana", "Original", "ana@example.com", ca, YEAR))), "a.xlsx", 202);
        JsonNode result = importFile(b, xlsx(IMPORT_HEADERS, List.of(row(id, "Otro", "Nombre", "", cb, YEAR))), "b.xlsx", 202);
        assertThat(result.get("successfulRows").asInt()).isEqualTo(1);

        // B obtiene su propio estudiante; el de A no cambia ni se matricula en el grupo de B
        long studentOfA = listStudents(a, ca.groupId()).get(0).id();
        long studentOfB = listStudents(b, cb.groupId()).get(0).id();
        assertThat(studentOfB).isNotEqualTo(studentOfA);
        JsonNode detailOfA = get(a, "/api/v1/students/" + studentOfA, 200);
        assertThat(detailOfA.toString()).contains("Original").doesNotContain("Nombre");
        assertThat(jdbc.queryForObject("select count(*) from student_groups where student_id = ?", Integer.class,
                studentOfA)).isEqualTo(1);
        assertError(get(b, "/api/v1/students/" + studentOfA, 404), 404, "RESOURCE_NOT_FOUND");
    }

    @Test
    void importsAreListedWithPaginationAndAreVisibleOnlyToTheirOwner() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        long first = importFile(t, xlsx(IMPORT_HEADERS, List.of(row(unique("ID"), "A", "B", "", c, YEAR))), "1.xlsx", 202).get("id").asLong();
        importFile(t, xlsx(IMPORT_HEADERS, List.of(row(unique("ID"), "C", "D", "", c, YEAR))), "2.xlsx", 202);

        JsonNode page = get(t, "/api/v1/imports?page=0&size=1", 200);
        assertThat(page.get("content").size()).isEqualTo(1);
        assertThat(page.get("totalElements").asInt()).isEqualTo(2);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        assertThat(get(t, "/api/v1/imports/" + first, 200).get("fileName").asText()).isEqualTo("1.xlsx");
        assertError(get(newTeacher(), "/api/v1/imports/" + first, 404), 404, "RESOURCE_NOT_FOUND");
    }

    @Test
    void templateHasTheExpectedColumns() {
        Teacher t = newTeacher();
        MvcResult result = download(t, "/api/v1/imports/students/template");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Sheet sheet = firstSheet(result.getResponse().getContentAsByteArray());
        List<String> headers = new ArrayList<>();
        sheet.getRow(0).forEach(cell -> headers.add(cell.getStringCellValue()));
        assertThat(sheet.getSheetName()).isEqualTo("Estudiantes");
        assertThat(headers).containsExactly("Número de identificación", "Nombres", "Apellidos", "Correo electrónico",
                "Grado", "Grupo", "Año lectivo");
        assertThat(result.getResponse().getHeader("Content-Disposition")).contains("plantilla-estudiantes.xlsx");
    }

    @Test
    void importsSpanishHeadersWithOrWithoutAccents() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        List<String> accented = List.of("Número de identificación", "Nombres", "Apellidos", "Correo electrónico", "Grado",
                "Grupo", "Año lectivo");
        List<String> plain = List.of("NUMERO DE IDENTIFICACION", "nombres", "apellidos", "correo electronico", "grado",
                "grupo", "ano lectivo");
        for (List<String> headers : List.of(accented, plain)) {
            JsonNode result = importFile(t, xlsx(headers, List.of(row(unique("ID"), "Ana", "Pérez", "", c, YEAR))),
                    "estudiantes.xlsx", 202);
            assertThat(result.get("errors")).as(headers.toString()).isEmpty();
            assertThat(result.get("successfulRows").asInt()).as(headers.toString()).isEqualTo(1);
        }
        assertThat(get(t, "/api/v1/students?groupId=" + c.groupId(), 200).get("totalElements").asInt()).isEqualTo(2);
    }

    @Test
    void uploadIsAcceptedImmediatelyAndTheResultIsQueriedLater() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        JsonNode accepted = parse(upload(t, "/api/v1/imports/students", "file", "students.xlsx", XLSX,
                xlsx(IMPORT_HEADERS, List.of(row(unique("ID"), "Ana", "Pérez", "", c, YEAR))), Map.of()), 202);
        assertThat(accepted.get("status").asText()).isEqualTo("QUEUED");
        assertThat(accepted.get("type").asText()).isEqualTo("STUDENTS");
        assertThat(accepted.get("totalRows").isNull()).isTrue();

        JsonNode done = awaitImport(t, accepted.get("id").asLong());
        assertThat(done.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(done.get("startedAt").isNull()).isFalse();
        assertThat(done.get("errorsTruncated").asBoolean()).isFalse();
    }

    @Test
    void aTeacherCannotStartASecondImportWhileOneIsInProgress() {
        Teacher t = newTeacher();
        Teacher other = newTeacher();
        Context c = newContext(t);
        jdbc.update("insert into import_batches (user_id, import_type, file_name, status, created_at) "
                + "values (?, 'STUDENTS', 'en-curso.xlsx', 'PROCESSING', now())", t.id());
        byte[] file = xlsx(IMPORT_HEADERS, List.of(row(unique("ID"), "Ana", "Pérez", "", c, YEAR)));

        assertError(importFile(t, file, "students.xlsx", 409), 409, "IMPORT_IN_PROGRESS");
        // solo bloquea al propio profesor
        assertThat(importFile(other, xlsx(IMPORT_HEADERS, List.of()), "students.xlsx", 202).get("status").asText())
                .isEqualTo("COMPLETED");
    }

    @Test
    void queuedImportsAreResumedAndLegacyUnfinishedImportsAreIgnored() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        // una importación síncrona antigua que un reinicio dejó colgada (sin tipo): ni bloquea ni se reprocesa
        long legacy = jdbc.queryForObject("insert into import_batches (user_id, file_name, status, created_at) "
                + "values (?, 'antigua.xlsx', 'PROCESSING', now()) returning id", Long.class, t.id());
        String path = storage.store("imports", "pendiente.xlsx",
                xlsx(IMPORT_HEADERS, List.of(row(unique("ID"), "Ana", "Pérez", "", c, YEAR))));
        long queued = jdbc.queryForObject("insert into import_batches (user_id, import_type, file_name, file_path, "
                + "status, created_at) values (?, 'STUDENTS', 'pendiente.xlsx', ?, 'QUEUED', now()) returning id",
                Long.class, t.id(), path);

        importWorker.resumeUnfinished();

        JsonNode resumed = awaitImport(t, queued);
        assertThat(resumed.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(resumed.get("successfulRows").asInt()).isEqualTo(1);
        assertThat(get(t, "/api/v1/imports/" + legacy, 200).get("status").asText()).isEqualTo("PROCESSING");
        assertThat(get(t, "/api/v1/students?groupId=" + c.groupId(), 200).get("totalElements").asInt()).isEqualTo(1);
    }
}
