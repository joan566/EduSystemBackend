package com.edusistem.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import com.edusistem.core.shared.domain.vo.TabularData;
import com.edusistem.core.shared.infrastructure.adapter.PoiSpreadsheetWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/** Base de los tests de integración: contexto completo de Spring, Flyway y PostgreSQL embebido reales. */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
public abstract class IntegrationTest {

    protected static final AtomicInteger SEQ = new AtomicInteger();
    protected static final String PASSWORD = "Secret123";
    protected static final int YEAR = LocalDate.now().getYear();

    public record Teacher(long id, String email, String token) {
    }

    /** Estructura académica completa de un profesor, con configuración de calificación 60/20/20 en escala 0-5. */
    public record Context(long gradeId, long groupId, long subjectId, long assignmentId, long academicPeriodId,
                             long teachingPeriodId, String gradeName, String groupName) {
    }

    public record Student(long id, String code, String identification) {
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        registry.add("spring.datasource.url", TestPostgres::url);
        registry.add("spring.datasource.username", TestPostgres::username);
        registry.add("spring.datasource.password", TestPostgres::password);
        registry.add("edusistem.security.jwt.secret", () -> "test-secret-test-secret-test-secret-1234");
        registry.add("edusistem.cors.allowed-origins", () -> "http://localhost:3000,https://*.edusistem.test");
        Path storage = Files.createTempDirectory("edusistem-test-storage");
        registry.add("edusistem.storage.base-path", storage::toString);
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected JdbcTemplate jdbc;

    // ----------------------------------------------------------------- HTTP helpers

    protected static String unique(String prefix) {
        return prefix + SEQ.incrementAndGet() + "x" + Integer.toHexString((int) (System.nanoTime() & 0xFFFF));
    }

    protected Teacher newTeacher() {
        String email = unique("teacher") + "@example.com";
        JsonNode body = call("POST", null, "/api/v1/auth/register", Map.of("firstName", "Test", "lastName", "Teacher",
                "email", email, "password", PASSWORD), 201);
        return new Teacher(body.get("user").get("id").asLong(), email, body.get("accessToken").asText());
    }

    protected MvcResult perform(String method, Teacher teacher, String url, Object body) {
        try {
            MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), url);
            if (teacher != null) {
                builder.header("Authorization", "Bearer " + teacher.token());
            }
            if (body != null) {
                builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
            }
            return mvc.perform(builder).andReturn();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected JsonNode call(String method, Teacher teacher, String url, Object body, int expectedStatus) {
        MvcResult result = perform(method, teacher, url, body);
        return parse(result, expectedStatus);
    }

    protected JsonNode parse(MvcResult result, int expectedStatus) {
        try {
            String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(result.getResponse().getStatus()).as("status of %s %s -> %s",
                    result.getRequest().getMethod(), result.getRequest().getRequestURI(), content).isEqualTo(expectedStatus);
            return content.isBlank() ? json.nullNode() : json.readTree(content);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected JsonNode get(Teacher t, String url, int status) {
        return call("GET", t, url, null, status);
    }

    protected JsonNode post(Teacher t, String url, Object body, int status) {
        return call("POST", t, url, body, status);
    }

    protected JsonNode put(Teacher t, String url, Object body, int status) {
        return call("PUT", t, url, body, status);
    }

    protected void assertError(JsonNode error, int status, String code) {
        assertThat(error.get("status").asInt()).isEqualTo(status);
        assertThat(error.get("code").asText()).isEqualTo(code);
        assertThat(error.has("timestamp")).isTrue();
        assertThat(error.has("path")).isTrue();
        assertThat(error.has("message")).isTrue();
    }

    protected MvcResult upload(Teacher teacher, String url, String part, String fileName, String contentType,
                               byte[] content, Map<String, String> params) {
        try {
            MockMultipartHttpServletRequestBuilder builder = multipart(url);
            builder.file(new MockMultipartFile(part, fileName, contentType, content));
            params.forEach(builder::param);
            if (teacher != null) {
                builder.header("Authorization", "Bearer " + teacher.token());
            }
            return mvc.perform(builder).andReturn();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected MvcResult download(Teacher teacher, String url) {
        return perform("GET", teacher, url, null);
    }

    // ----------------------------------------------------------------- escenarios

    protected Context newContext(Teacher t) {
        String gradeName = unique("G");
        long grade = post(t, "/api/v1/grades", Map.of("name", gradeName), 201).get("id").asLong();
        String groupName = unique("Grp");
        long group = post(t, "/api/v1/groups", Map.of("gradeId", grade, "name", groupName, "academicYear", YEAR), 201)
                .get("id").asLong();
        return contextFor(t, group, grade, gradeName, groupName);
    }

    /** Asigna al profesor un grupo existente + asignatura nueva + periodo + configuración 60/20/20 (escala 0-5). */
    protected Context contextFor(Teacher t, long group, long grade, String gradeName, String groupName) {
        long subject = post(t, "/api/v1/subjects", Map.of("name", unique("Subj")), 201).get("id").asLong();
        long assignment = post(t, "/api/v1/teaching-assignments", Map.of("groupId", group, "subjectId", subject), 201)
                .get("id").asLong();
        long period = post(t, "/api/v1/academic-periods", Map.of("name", unique("P"), "startDate", YEAR + "-01-15",
                "endDate", YEAR + "-04-01"), 201).get("id").asLong();
        long tp = post(t, "/api/v1/teaching-periods", Map.of("teachingAssignmentId", assignment,
                "academicPeriodId", period), 201).get("id").asLong();
        put(t, "/api/v1/teaching-periods/" + tp + "/grading-configuration", Map.of("gradingScaleId", scaleId("Colombian 0-5"),
                "weights", List.of(Map.of("evaluationCategoryId", categoryId("EXAMS"), "weight", 60),
                        Map.of("evaluationCategoryId", categoryId("ACTIVITIES"), "weight", 20),
                        Map.of("evaluationCategoryId", categoryId("ATTENDANCE"), "weight", 20))), 200);
        return new Context(grade, group, subject, assignment, period, tp, gradeName, groupName);
    }

    protected long scaleId(String name) {
        return jdbc.queryForObject("select id from grading_scales where name = ?", Long.class, name);
    }

    protected long categoryId(String name) {
        return jdbc.queryForObject("select id from evaluation_categories where name = ?", Long.class, name);
    }

    protected byte[] xlsx(List<String> headers, List<List<Object>> rows) {
        return new PoiSpreadsheetWriter().write(new TabularData("Students", headers, rows));
    }

    protected byte[] xlsxWorkbook(List<TabularData> sheets) {
        return new PoiSpreadsheetWriter().writeWorkbook(sheets);
    }

    protected static final List<String> IMPORT_HEADERS = List.of("identification_number", "first_name", "last_name",
            "email", "grade", "group", "academic_year");

    /** Importa {@code count} estudiantes al grupo y devuelve sus datos. */
    protected List<Student> importStudents(Teacher t, Context c, int count) {
        List<List<Object>> rows = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            rows.add(List.of(unique("ID"), "Nombre" + i, "Apellido" + i, "alumno" + SEQ.incrementAndGet() + "@example.com",
                    c.gradeName(), c.groupName(), YEAR));
        }
        MvcResult result = upload(t, "/api/v1/imports/students", "file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx(IMPORT_HEADERS, rows), Map.of());
        JsonNode body = parse(result, 201);
        assertThat(body.get("successfulRows").asInt()).isEqualTo(count);
        return listStudents(t, c.groupId());
    }

    protected List<Student> listStudents(Teacher t, long groupId) {
        JsonNode page = get(t, "/api/v1/students?groupId=" + groupId + "&size=100", 200);
        List<Student> students = new ArrayList<>();
        page.get("content").forEach(n -> students.add(new Student(n.get("id").asLong(), n.get("studentCode").asText(),
                n.get("identificationNumber").asText())));
        return students;
    }

    protected Sheet firstSheet(byte[] xlsx) {
        try {
            Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsx));
            return workbook.getSheetAt(0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Sheet sheet(byte[] xlsx, String name) {
        try {
            Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsx));
            return workbook.getSheet(name);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
