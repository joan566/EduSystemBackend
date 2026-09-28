package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Importación de preguntas desde Word (.docx) y cuadernillo de preguntas generado a partir de ellas. */
class ExamDocumentImportIntegrationTest extends IntegrationTest {

    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static byte[] docx(String... paragraphs) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String p : paragraphs) {
                document.createParagraph().createRun().setText(p);
            }
            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final byte[] BIOLOGY = docx(
            "Examen de Biología",
            "Instrucciones: lea con atención.",
            "1. ¿Cuál es el órgano que bombea la sangre?",
            "A) Pulmón", "B) Corazón", "C) Hígado",
            "Respuesta: B",
            "2. La unidad básica de la vida es",
            "A) El átomo", "*B) La célula", "C) El tejido",
            "Puntos: 3",
            "3. Si π ≈ 3,14 y r = 2, el área del círculo es aproximadamente",
            "A) 6,28", "B) 12,56", "C) 3,14",
            "Respuesta: B");

    private JsonNode importExam(Teacher t, Context c, byte[] file, Map<String, String> extra, int status) {
        Map<String, String> params = new java.util.HashMap<>(extra);
        params.put("teachingPeriodId", String.valueOf(c.teachingPeriodId()));
        return parse(upload(t, "/api/v1/exams/import", "file", "Parcial Biología.docx", DOCX, file, params), status);
    }

    @Test
    void createsAReadyExamFromAWordDocument() throws IOException {
        Teacher t = newTeacher();
        Context c = newContext(t);
        JsonNode exam = importExam(t, c, BIOLOGY, Map.of(), 201);
        assertThat(exam.get("name").asText()).isEqualTo("Parcial Biología"); // tomado del nombre del archivo
        assertThat(exam.get("ready").asBoolean()).isTrue();
        assertThat(exam.get("numberOfQuestions").asInt()).isEqualTo(3);
        assertThat(exam.get("optionCount").asInt()).isEqualTo(3);
        JsonNode questions = exam.get("questions");
        assertThat(questions.get(0).get("correctOption").asText()).isEqualTo("B");
        assertThat(questions.get(1).get("correctOption").asText()).isEqualTo("B");
        assertThat(questions.get(1).get("options").get(1).get("text").asText()).isEqualTo("La célula");
        assertThat(questions.get(1).get("points").decimalValue()).isEqualByComparingTo("3");

        // el cuadernillo imprime los enunciados tal cual (con símbolos) y no revela las respuestas
        MvcResult booklet = download(t, "/api/v1/exams/" + exam.get("id").asLong() + "/question-booklet");
        try (PDDocument doc = Loader.loadPDF(booklet.getResponse().getContentAsByteArray())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("¿Cuál es el órgano que bombea la sangre?").contains("π ≈ 3,14")
                    .contains("La célula").doesNotContain("Respuesta:").doesNotContain("*");
        }
    }

    @Test
    void previewInterpretsTheDocumentWithoutSavingAndTheTemplateIsImportable() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        JsonNode preview = parse(upload(t, "/api/v1/exams/import/preview", "file", "x.docx", DOCX, BIOLOGY, Map.of()), 200);
        assertThat(preview.get("numberOfQuestions").asInt()).isEqualTo(3);
        assertThat(preview.get("questions").get(0).get("points").isNull()).isTrue();
        assertThat(get(t, "/api/v1/exams?teachingPeriodId=" + c.teachingPeriodId(), 200).get("totalElements").asInt()).isZero();

        MvcResult template = download(t, "/api/v1/exams/import/template");
        assertThat(template.getResponse().getContentType()).isEqualTo(DOCX);
        byte[] templateFile = template.getResponse().getContentAsByteArray();
        JsonNode fromTemplate = parse(upload(t, "/api/v1/exams/import/preview", "file", "plantilla.docx", DOCX,
                templateFile, Map.of()), 200);
        assertThat(fromTemplate.get("numberOfQuestions").asInt()).isEqualTo(2);
        assertThat(fromTemplate.get("questions").get(1).get("points").decimalValue()).isEqualByComparingTo("2");
    }

    @Test
    void invalidDocumentsAreRejectedWithTheListOfProblems() throws IOException {
        Teacher t = newTeacher();
        Context c = newContext(t);
        JsonNode error = importExam(t, c, docx("1. Sin respuesta", "A) Uno", "B) Dos", "2. Otra", "A) Uno"), Map.of(), 400);
        assertError(error, 400, "INVALID_QUESTION_DOCUMENT");
        assertThat(error.get("errors")).hasSize(3); // q1 sin respuesta; q2 con una sola opción y sin respuesta
        assertThat(error.get("errors").get(0).get("field").asText()).isEqualTo("question 1");
        assertThat(get(t, "/api/v1/exams?teachingPeriodId=" + c.teachingPeriodId(), 200).get("totalElements").asInt()).isZero();

        ByteArrayOutputStream legacy = new ByteArrayOutputStream();
        try (POIFSFileSystem fs = new POIFSFileSystem()) {
            fs.writeFilesystem(legacy);
        }
        assertError(importExam(t, c, legacy.toByteArray(), Map.of(), 400), 400, "UNSUPPORTED_DOCUMENT_FORMAT");
        assertError(importExam(t, c, "no soy un word".getBytes(), Map.of(), 400), 400, "INVALID_DOCUMENT");
    }

    @Test
    void replacesTheQuestionsOfAnExistingExamAndRespectsOwnership() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        long examId = importExam(t, c, BIOLOGY, Map.of("name", "Quiz", "maximumScore", "5"), 201).get("id").asLong();
        byte[] shorter = docx("1. ¿2 + 2?", "A) 3", "B) 4", "Respuesta: B", "2. ¿3 + 3?", "A) 6", "B) 7", "Respuesta: A");
        JsonNode replaced = parse(upload(t, "/api/v1/exams/" + examId + "/questions/import", "file", "q.docx", DOCX,
                shorter, Map.of()), 200);
        assertThat(replaced.get("name").asText()).isEqualTo("Quiz");
        assertThat(replaced.get("numberOfQuestions").asInt()).isEqualTo(2);
        assertThat(replaced.get("optionCount").asInt()).isEqualTo(2);

        Teacher other = newTeacher();
        assertError(parse(upload(other, "/api/v1/exams/" + examId + "/questions/import", "file", "q.docx", DOCX,
                shorter, Map.of()), 404), 404, "RESOURCE_NOT_FOUND");
        assertError(parse(upload(other, "/api/v1/exams/import", "file", "q.docx", DOCX, shorter,
                Map.of("teachingPeriodId", String.valueOf(c.teachingPeriodId()))), 404), 404, "RESOURCE_NOT_FOUND");
    }

    @Test
    void examsWithManyQuestionsSpreadTheBookletOverSeveralPages() throws IOException {
        Teacher t = newTeacher();
        Context c = newContext(t);
        List<String> lines = new java.util.ArrayList<>();
        for (int q = 1; q <= 40; q++) {
            lines.add(q + ". Enunciado largo de la pregunta " + q + " que ocupa bastante espacio para comprobar que el "
                    + "texto se reparte en varias líneas sin salirse del margen derecho de la página.");
            lines.addAll(List.of("A) Primera opción", "B) Segunda opción", "C) Tercera opción", "D) Cuarta opción",
                    "Respuesta: C"));
        }
        long examId = importExam(t, c, docx(lines.toArray(String[]::new)), Map.of(), 201).get("id").asLong();
        try (PDDocument doc = Loader.loadPDF(download(t, "/api/v1/exams/" + examId + "/question-booklet")
                .getResponse().getContentAsByteArray())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(3);
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Enunciado largo de la pregunta 40").contains("Página 1 de " + doc.getNumberOfPages());
        }
    }
}
