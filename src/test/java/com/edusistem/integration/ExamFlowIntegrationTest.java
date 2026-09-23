package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.core.exam.domain.vo.AnswerSheetData;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import com.edusistem.core.exam.infrastructure.adapter.omr.AnswerSheetProcessorAdapter;
import com.edusistem.core.exam.infrastructure.adapter.pdf.PdfAnswerSheetRenderer;
import com.edusistem.support.IntegrationTest;
import com.edusistem.support.SheetImages;
import com.fasterxml.jackson.databind.JsonNode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Flujo completo: examen → hoja PDF → foto → QR → burbujas → puntuación → nota → revisión manual. */
class ExamFlowIntegrationTest extends IntegrationTest {

    private static final float DPI = 200;
    private static final int N = 10;
    private static final AnswerSheetLayout LAYOUT = new AnswerSheetLayout(N, 4);
    private static final String CORRECT = "ABCDABCDAB";
    private static final String JPEG = "image/jpeg";

    record ExamFixture(Teacher teacher, Context context, List<Student> students, long examId) {
    }

    private List<Map<String, Object>> questions(int n) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            list.add(Map.of("questionNumber", i, "statement", "Pregunta " + i + ": ¿cuál es la respuesta?",
                    "correctOption", String.valueOf(CORRECT.charAt((i - 1) % CORRECT.length())),
                    "options", List.of(Map.of("letter", "A", "text", "Opción A"), Map.of("letter", "B", "text", "Opción B"),
                            Map.of("letter", "C", "text", "Opción C"), Map.of("letter", "D", "text", "Opción D"))));
        }
        return list;
    }

    private long createExam(Teacher t, Context c, String name) {
        return post(t, "/api/v1/exams", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", name, "maximumScore", 5,
                "numberOfQuestions", N, "questions", questions(N)), 201).get("id").asLong();
    }

    private ExamFixture fixture() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        List<Student> students = importStudents(t, c, 3);
        return new ExamFixture(t, c, students, createExam(t, c, unique("Parcial ")));
    }

    private byte[] sheetPdf(Teacher t, long examId, Student s) {
        MvcResult result = download(t, "/api/v1/exams/" + examId + "/answer-sheet/" + s.id());
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return result.getResponse().getContentAsByteArray();
    }

    /** Fotografía de la hoja del estudiante con las respuestas indicadas (-1 = sin marcar; índice de opción base 0). */
    private BufferedImage answeredSheet(ExamFixture f, Student s, int[] choices) {
        BufferedImage image = SheetImages.render(sheetPdf(f.teacher(), f.examId(), s), 0, DPI);
        for (int q = 1; q <= N; q++) {
            if (choices[q - 1] >= 0) {
                SheetImages.mark(image, LAYOUT, DPI, q, choices[q - 1]);
            }
        }
        return image;
    }

    private int[] correctChoices() {
        int[] choices = new int[N];
        for (int i = 0; i < N; i++) {
            choices[i] = CORRECT.charAt(i) - 'A';
        }
        return choices;
    }

    /** Respuestas correctas salvo en las preguntas indicadas (base 1), que se marcan con otra opción. */
    private int[] correctExcept(int... wrong) {
        int[] choices = correctChoices();
        for (int q : wrong) {
            choices[q - 1] = (choices[q - 1] + 1) % 4;
        }
        return choices;
    }

    private MvcResult submit(Teacher t, long examId, BufferedImage image, Map<String, String> params) {
        return upload(t, "/api/v1/exams/" + examId + "/submissions", "image", "photo.jpg", JPEG, SheetImages.jpeg(image), params);
    }

    private JsonNode submitOk(ExamFixture f, Student s, int[] choices) {
        return parse(submit(f.teacher(), f.examId(), answeredSheet(f, s, choices), Map.of()), 201);
    }

    private JsonNode answer(JsonNode submission, int question) {
        return submission.get("answers").get(question - 1);
    }

    // ----------------------------------------------------------------------------------------------

    @Test
    void examCreationStoresQuestionsOptionsCorrectAnswersAndPoints() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        JsonNode exam = get(t, "/api/v1/exams/" + createExam(t, c, "Parcial 1"), 200);
        assertThat(exam.get("numberOfQuestions").asInt()).isEqualTo(N);
        assertThat(exam.get("ready").asBoolean()).isTrue();
        assertThat(exam.get("optionCount").asInt()).isEqualTo(4);
        assertThat(exam.get("maximumScore").decimalValue()).isEqualByComparingTo("5");
        JsonNode q1 = exam.get("questions").get(0);
        assertThat(q1.get("correctOption").asText()).isEqualTo("A");
        assertThat(q1.get("points").decimalValue()).isEqualByComparingTo("0.5"); // 5 puntos / 10 preguntas
        assertThat(q1.get("options").size()).isEqualTo(4);
        assertThat(jdbc.queryForObject("select count(*) from exam_question_options o join exam_questions q on q.id = o.question_id "
                + "where q.exam_id = ?", Integer.class, exam.get("id").asLong())).isEqualTo(N * 4);
        assertThat(jdbc.queryForObject("select category.name from evaluations e join evaluation_categories category "
                + "on category.id = e.evaluation_category_id where e.id = ?", String.class, exam.get("evaluationId").asLong())).isEqualTo("EXAMS");
    }

    @Test
    void examCreationRejectsInvalidQuestions() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        List<Map<String, Object>> gap = questions(3);
        gap.set(2, Map.of("questionNumber", 5, "statement", "x", "correctOption", "A",
                "options", List.of(Map.of("letter", "A", "text", "a"), Map.of("letter", "B", "text", "b"))));
        assertError(post(t, "/api/v1/exams", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "E", "numberOfQuestions", 3,
                "questions", gap), 400), 400, "INVALID_QUESTION_NUMBERING");
        List<Map<String, Object>> fewerOptions = new ArrayList<>(questions(2));
        fewerOptions.set(1, Map.of("questionNumber", 2, "statement", "x", "correctOption", "A",
                "options", List.of(Map.of("letter", "A", "text", "a"), Map.of("letter", "B", "text", "b"))));
        assertError(post(t, "/api/v1/exams", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "E", "numberOfQuestions", 2,
                "questions", fewerOptions), 400), 400, "INVALID_OPTIONS");
        List<Map<String, Object>> badCorrect = new ArrayList<>(questions(2));
        badCorrect.set(0, Map.of("questionNumber", 1, "statement", "x", "correctOption", "F", "options", List.of(
                Map.of("letter", "A", "text", "a"), Map.of("letter", "B", "text", "b"), Map.of("letter", "C", "text", "c"), Map.of("letter", "D", "text", "d"))));
        assertError(post(t, "/api/v1/exams", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "E", "numberOfQuestions", 2,
                "questions", badCorrect), 400), 400, "INVALID_CORRECT_OPTION");
        assertError(post(t, "/api/v1/exams", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "E", "numberOfQuestions", 5,
                "questions", questions(3)), 400), 400, "QUESTION_COUNT_MISMATCH");
        assertError(post(t, "/api/v1/exams", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "E", "numberOfQuestions", 0), 400),
                400, "INVALID_REQUEST");
        assertThat(get(t, "/api/v1/exams?teachingPeriodId=" + c.teachingPeriodId(), 200).get("totalElements").asInt()).isZero();
    }

    @Test
    void answerSheetPdfIsGeneratedForAStudentAndForTheWholeGroupWithAValidQr() throws IOException {
        ExamFixture f = fixture();
        Student s = f.students().get(0);
        MvcResult one = download(f.teacher(), "/api/v1/exams/" + f.examId() + "/answer-sheet/" + s.id());
        assertThat(one.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(one.getResponse().getHeader("Content-Disposition")).contains("attachment");
        byte[] pdf = one.getResponse().getContentAsByteArray();
        assertThat(SheetImages.pageCount(pdf)).isEqualTo(1);

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("HOJA DE RESPUESTAS").contains("Apellido").contains(s.code()).contains("Asignatura");
        }
        // el QR contiene solo versión, examen y código de estudiante (sin datos personales)
        String qr = new AnswerSheetProcessorAdapter().readQrCode(SheetImages.png(SheetImages.render(pdf, 0, DPI))).orElseThrow();
        assertThat(qr).isEqualTo("EDU1|" + f.examId() + "|" + s.code());

        byte[] group = download(f.teacher(), "/api/v1/exams/" + f.examId() + "/answer-sheets").getResponse().getContentAsByteArray();
        assertThat(SheetImages.pageCount(group)).isEqualTo(3);
        // un estudiante que no pertenece al grupo no tiene hoja
        Student outsider = importStudents(f.teacher(), newContext(f.teacher()), 1).get(0);
        assertError(parse(download(f.teacher(), "/api/v1/exams/" + f.examId() + "/answer-sheet/" + outsider.id()), 404), 404, "RESOURCE_NOT_FOUND");
    }

    @Test
    void answerSheetsRequireTheQuestionsToBeDefinedFirst() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        Student s = importStudents(t, c, 1).get(0);
        long exam = post(t, "/api/v1/exams", Map.of("teachingPeriodId", c.teachingPeriodId(), "name", "Sin preguntas",
                "numberOfQuestions", 4), 201).get("id").asLong();
        assertError(parse(download(t, "/api/v1/exams/" + exam + "/answer-sheet/" + s.id()), 422), 422, "EXAM_NOT_READY");
        put(t, "/api/v1/exams/" + exam + "/questions", Map.of("questions", questions(4)), 200);
        assertThat(download(t, "/api/v1/exams/" + exam + "/answer-sheet/" + s.id()).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void correctSheetIsProcessedScoredAndConvertedToTheScale() {
        ExamFixture f = fixture();
        Student s = f.students().get(0);
        JsonNode result = submitOk(f, s, correctExcept(3, 7)); // 8 correctas, 2 incorrectas

        assertThat(result.get("status").asText()).isEqualTo("PROCESSED");
        assertThat(result.get("student").get("studentCode").asText()).isEqualTo(s.code());
        assertThat(result.get("score").decimalValue()).isEqualByComparingTo("4.00");         // puntuación bruta
        assertThat(result.get("maximumScore").decimalValue()).isEqualByComparingTo("5");
        assertThat(result.get("finalGrade").decimalValue()).isEqualByComparingTo("4.00");    // 8/10 sobre 0-5
        assertThat(result.get("scaleMaximum").decimalValue()).isEqualByComparingTo("5");
        assertThat(result.get("answers").size()).isEqualTo(N);
        assertThat(answer(result, 1).get("correct").asBoolean()).isTrue();
        assertThat(answer(result, 3).get("correct").asBoolean()).isFalse();
        assertThat(answer(result, 3).get("selectedOption").asText()).isEqualTo("D");
        assertThat(answer(result, 3).get("correctOption").asText()).isEqualTo("C");
        assertThat(answer(result, 1).get("detectionStatus").asText()).isEqualTo("MARKED");
        assertThat(answer(result, 1).get("detectionConfidence").decimalValue()).isGreaterThanOrEqualTo(new java.math.BigDecimal("0.90"));

        long id = result.get("id").asLong();
        Map<String, Object> row = jdbc.queryForMap("select * from exam_submissions where id = ?", id);
        assertThat(row.get("student_code")).isEqualTo(s.code());
        assertThat(row.get("detected_qr_data")).isEqualTo("EDU1|" + f.examId() + "|" + s.code());
        assertThat(row.get("image_path")).isNotNull();
        assertThat(row.get("processed_at")).isNotNull();
        assertThat(jdbc.queryForObject("select count(*) from exam_answers where submission_id = ?", Integer.class, id)).isEqualTo(N);
        assertThat(jdbc.queryForObject("select count(*) from exam_answers where submission_id = ? and is_correct", Integer.class, id)).isEqualTo(8);
        assertThat(get(f.teacher(), "/api/v1/audit-logs?action=EXAM_PROCESSED", 200).get("content").toString()).contains("SUCCESS");

        JsonNode detail = get(f.teacher(), "/api/v1/exams/" + f.examId() + "/submissions/" + id, 200);
        assertThat(detail.get("answers").size()).isEqualTo(N);
        assertThat(detail.get("examName").asText()).startsWith("Parcial");
    }

    @Test
    void finalGradeFollowsTheConfiguredScaleNotTheRawScore() {
        Teacher t = newTeacher();
        Context c = newContext(t);
        Student s = importStudents(t, c, 1).get(0);
        put(t, "/api/v1/teaching-periods/" + c.teachingPeriodId() + "/grading-configuration", Map.of("gradingScaleId", scaleId("Percentage 0-100"),
                "weights", List.of(Map.of("evaluationCategoryId", categoryId("EXAMS"), "weight", 100))), 200);
        ExamFixture f = new ExamFixture(t, c, List.of(s), createExam(t, c, "Parcial %"));
        JsonNode result = submitOk(f, s, correctExcept(1, 2, 3)); // 7 de 10
        assertThat(result.get("score").decimalValue()).isEqualByComparingTo("3.50");
        assertThat(result.get("finalGrade").decimalValue()).isEqualByComparingTo("70.00");
        // con calificaciones ya registradas no se puede cambiar la escala del periodo
        assertError(put(t, "/api/v1/teaching-periods/" + c.teachingPeriodId() + "/grading-configuration", Map.of("gradingScaleId", scaleId("Scale 0-10"),
                "weights", List.of(Map.of("evaluationCategoryId", categoryId("EXAMS"), "weight", 100))), 409), 409, "GRADING_SCALE_LOCKED");
    }

    @Test
    void emptyMultipleAndWeakMarksRequireReviewAndTheTeacherCanFixThemWithAudit() {
        ExamFixture f = fixture();
        Student s = f.students().get(1);
        int[] choices = correctChoices();
        choices[1] = -1;                                   // pregunta 2 sin marcar
        BufferedImage image = SheetImages.render(sheetPdf(f.teacher(), f.examId(), s), 0, DPI);
        for (int q = 1; q <= N; q++) {
            if (choices[q - 1] >= 0 && q != 3 && q != 4) {
                SheetImages.mark(image, LAYOUT, DPI, q, choices[q - 1]);
            }
        }
        SheetImages.mark(image, LAYOUT, DPI, 3, 2);        // pregunta 3: C y D (doble marca)
        SheetImages.mark(image, LAYOUT, DPI, 3, 3);
        SheetImages.fill(image, LAYOUT, DPI, 4, 3, 2.6, 60);  // pregunta 4: marca pequeña/débil en D (la correcta)

        JsonNode result = parse(submit(f.teacher(), f.examId(), image, Map.of()), 201);
        assertThat(result.get("status").asText()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.get("statusDetail").asText()).contains("2 answer(s)");
        JsonNode q2 = answer(result, 2);
        assertThat(q2.get("detectionStatus").asText()).isEqualTo("EMPTY");
        assertThat(q2.get("selectedOption").isNull()).isTrue();
        assertThat(q2.get("correct").isNull()).isTrue();          // vacía: no se marca incorrecta
        JsonNode q3 = answer(result, 3);
        assertThat(q3.get("detectionStatus").asText()).isEqualTo("MULTIPLE_MARK");
        assertThat(q3.get("selectedOption").isNull()).isTrue();   // no se elige una arbitrariamente
        assertThat(q3.get("needsReview").asBoolean()).isTrue();
        JsonNode q4 = answer(result, 4);
        assertThat(q4.get("detectionStatus").asText()).isEqualTo("REVIEW_REQUIRED");
        assertThat(q4.get("selectedOption").asText()).isEqualTo("D"); // candidata sugerida
        assertThat(q4.get("correct").isNull()).isTrue();
        assertThat(result.get("score").decimalValue()).isEqualByComparingTo("3.50"); // 7 correctas seguras × 0.5

        long id = result.get("id").asLong();
        String base = "/api/v1/exams/" + f.examId() + "/submissions/" + id;
        JsonNode afterQ3 = put(f.teacher(), base + "/answers/3", Map.of("selectedOption", "C", "reason", "el alumno tachó la D"), 200);
        assertThat(answer(afterQ3, 3).get("detectionStatus").asText()).isEqualTo("MANUAL");
        assertThat(answer(afterQ3, 3).get("correct").asBoolean()).isTrue();
        assertThat(afterQ3.get("status").asText()).isEqualTo("REVIEW_REQUIRED"); // falta la 4
        assertThat(afterQ3.get("score").decimalValue()).isEqualByComparingTo("4.00");

        JsonNode afterQ4 = put(f.teacher(), base + "/answers/4", Map.of("selectedOption", "d"), 200);
        assertThat(afterQ4.get("status").asText()).isEqualTo("PROCESSED");
        assertThat(afterQ4.get("score").decimalValue()).isEqualByComparingTo("4.50");
        assertThat(afterQ4.get("finalGrade").decimalValue()).isEqualByComparingTo("4.50");

        JsonNode blankConfirmed = put(f.teacher(), base + "/answers/2", Map.of("selectedOption", ""), 200);
        assertThat(answer(blankConfirmed, 2).get("detectionStatus").asText()).isEqualTo("MANUAL");
        assertThat(answer(blankConfirmed, 2).get("correct").asBoolean()).isFalse();
        assertThat(blankConfirmed.get("score").decimalValue()).isEqualByComparingTo("4.50");

        assertError(put(f.teacher(), base + "/answers/3", Map.of("selectedOption", "Z"), 400), 400, "INVALID_OPTION");
        assertError(put(f.teacher(), base + "/answers/99", Map.of("selectedOption", "A"), 404), 404, "RESOURCE_NOT_FOUND");

        // edición manual de la nota final, dentro de la escala
        JsonNode graded = put(f.teacher(), base + "/final-grade", Map.of("finalGrade", 5.0, "reason", "bonificación"), 200);
        assertThat(graded.get("finalGrade").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(graded.get("score").decimalValue()).isEqualByComparingTo("4.50"); // la puntuación bruta se conserva
        assertError(put(f.teacher(), base + "/final-grade", Map.of("finalGrade", 5.5), 400), 400, "GRADE_OUT_OF_RANGE");

        // trazabilidad: quién, qué, valor anterior y nuevo
        String answerAudit = get(f.teacher(), "/api/v1/audit-logs?action=ANSWER_UPDATED&entityId=" + id, 200).get("content").toString();
        assertThat(answerAudit).contains("Q3: none(MULTIPLE_MARK").contains("-> C(MANUAL").contains("score 3.50 -> 4.00")
                .contains("el alumno tachó la D");
        String gradeAudit = get(f.teacher(), "/api/v1/audit-logs?action=GRADE_UPDATED&entityId=" + id, 200).get("content").toString();
        assertThat(gradeAudit).contains("finalGrade 4.50 -> 5.00").contains("bonificación");
    }

    @Test
    void sameStudentCannotBeSubmittedTwiceUnlessExplicitlyReplaced() {
        ExamFixture f = fixture();
        Student s = f.students().get(0);
        JsonNode first = submitOk(f, s, correctChoices());
        BufferedImage retake = answeredSheet(f, s, correctExcept(1, 2));

        assertError(parse(submit(f.teacher(), f.examId(), retake, Map.of()), 409), 409, "SUBMISSION_ALREADY_EXISTS");
        assertThat(get(f.teacher(), "/api/v1/exams/" + f.examId() + "/submissions/" + first.get("id").asLong(), 200)
                .get("score").decimalValue()).isEqualByComparingTo("5.00"); // intacta

        JsonNode replaced = parse(submit(f.teacher(), f.examId(), retake, Map.of("replace", "true")), 201);
        assertThat(replaced.get("id").asLong()).isEqualTo(first.get("id").asLong());
        assertThat(replaced.get("score").decimalValue()).isEqualByComparingTo("4.00");
        assertThat(jdbc.queryForObject("select count(*) from exam_submissions where exam_id = ? and student_id = ?", Integer.class,
                f.examId(), s.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from exam_answers where submission_id = ?", Integer.class,
                first.get("id").asLong())).isEqualTo(N);
    }

    @Test
    void qrIsValidatedAgainstExamStudentAndGroupBeforeSavingAnything() {
        ExamFixture f = fixture();
        Student s = f.students().get(0);
        long otherExam = createExam(f.teacher(), f.context(), "Otro examen");

        // hoja de otro examen
        BufferedImage otherExamSheet = SheetImages.render(sheetPdf(f.teacher(), otherExam, s), 0, DPI);
        assertError(parse(submit(f.teacher(), f.examId(), otherExamSheet, Map.of()), 422), 422, "QR_EXAM_MISMATCH");

        // el QR contradice al estudiante indicado
        BufferedImage sheet = answeredSheet(f, s, correctChoices());
        assertError(parse(submit(f.teacher(), f.examId(), sheet, Map.of("studentId", String.valueOf(f.students().get(1).id()))), 422),
                422, "STUDENT_MISMATCH");

        // QR con contenido inválido, estudiante inexistente y estudiante de otro grupo
        Student outsider = importStudents(f.teacher(), newContext(f.teacher()), 1).get(0);
        assertError(parse(submit(f.teacher(), f.examId(), craftedSheet("hello world"), Map.of()), 422), 422, "INVALID_QR");
        assertError(parse(submit(f.teacher(), f.examId(), craftedSheet("EDU1|" + f.examId() + "|EST-NOEXISTE"), Map.of()), 422), 422, "QR_STUDENT_NOT_FOUND");
        assertError(parse(submit(f.teacher(), f.examId(), craftedSheet("EDU1|" + f.examId() + "|" + outsider.code()), Map.of()), 422),
                422, "STUDENT_NOT_IN_GROUP");

        assertThat(jdbc.queryForObject("select count(*) from exam_submissions where exam_id = ?", Integer.class, f.examId())).isZero();
        String failures = get(f.teacher(), "/api/v1/audit-logs?action=EXAM_PROCESSED&size=50", 200).get("content").toString();
        assertThat(failures).contains("FAILURE").contains("QR_EXAM_MISMATCH").contains("STUDENT_NOT_IN_GROUP");
    }

    private BufferedImage craftedSheet(String qrContent) {
        byte[] pdf = new PdfAnswerSheetRenderer().render(List.of(new AnswerSheetData("Examen", "Materia", "Grupo",
                "Alumno", "EST-X", qrContent, LAYOUT)));
        return SheetImages.render(pdf, 0, DPI);
    }

    @Test
    void unreadableImagesNeverBlockTheSystemAndLeaveAnInspectableStatus() {
        ExamFixture f = fixture();
        Student s = f.students().get(0);
        BufferedImage blank = new BufferedImage(900, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = blank.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 900, 1200);
        g.dispose();

        // sin QR y sin estudiante indicado: se rechaza, no se guarda nada
        assertError(parse(submit(f.teacher(), f.examId(), blank, Map.of()), 422), 422, "QR_NOT_DETECTED");
        // con estudiante indicado manualmente pero sin hoja visible: queda FAILED con el motivo
        JsonNode failed = parse(submit(f.teacher(), f.examId(), blank, Map.of("studentId", String.valueOf(s.id()))), 201);
        assertThat(failed.get("status").asText()).isEqualTo("FAILED");
        assertThat(failed.get("statusDetail").asText()).contains("corner markers");
        assertThat(failed.get("answers")).isEmpty();
        assertThat(failed.get("finalGrade").isNull()).isTrue();
        assertThat(get(f.teacher(), "/api/v1/exams/" + f.examId() + "/submissions/" + failed.get("id").asLong(), 200)
                .get("status").asText()).isEqualTo("FAILED");
        assertError(put(f.teacher(), "/api/v1/exams/" + f.examId() + "/submissions/" + failed.get("id").asLong() + "/answers/1",
                Map.of("selectedOption", "A"), 409), 409, "SUBMISSION_NOT_REVIEWABLE");

        // se puede reintentar con una buena foto sin necesidad de replace (estaba FAILED)
        JsonNode retry = submitOk(f, s, correctChoices());
        assertThat(retry.get("id").asLong()).isEqualTo(failed.get("id").asLong());
        assertThat(retry.get("status").asText()).isEqualTo("PROCESSED");

        // archivos que no son imágenes
        assertError(parse(upload(f.teacher(), "/api/v1/exams/" + f.examId() + "/submissions", "image", "x.jpg", JPEG,
                "esto no es una imagen".getBytes(), Map.of()), 400), 400, "INVALID_IMAGE");
        assertError(parse(upload(f.teacher(), "/api/v1/exams/" + f.examId() + "/submissions", "image", "x.jpg", JPEG,
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5}, Map.of()), 400), 400, "INVALID_IMAGE");
    }

    @Test
    void photoWithShadowNoiseRotationAndUpsideDownIsProcessed() {
        ExamFixture f = fixture();
        BufferedImage sheet = answeredSheet(f, f.students().get(0), correctExcept(5));
        JsonNode result = parse(submit(f.teacher(), f.examId(), SheetImages.photo(SheetImages.rotate180(sheet), 6, 0.8), Map.of()), 201);
        assertThat(result.get("status").asText()).isEqualTo("PROCESSED");
        assertThat(result.get("score").decimalValue()).isEqualByComparingTo("4.50");
    }

    @Test
    void submissionsAreListedByStatusWithPaginationAndQuestionsAreFrozenAfterwards() {
        ExamFixture f = fixture();
        submitOk(f, f.students().get(0), correctChoices());
        int[] choices = correctChoices();
        choices[0] = -1;
        submitOk(f, f.students().get(1), choices); // una vacía -> sigue PROCESSED (EMPTY no exige revisión)

        JsonNode all = get(f.teacher(), "/api/v1/exams/" + f.examId() + "/submissions?size=1", 200);
        assertThat(all.get("totalElements").asInt()).isEqualTo(2);
        assertThat(all.get("content").size()).isEqualTo(1);
        assertThat(get(f.teacher(), "/api/v1/exams/" + f.examId() + "/submissions?status=REVIEW_REQUIRED", 200).get("totalElements").asInt()).isZero();
        assertThat(get(f.teacher(), "/api/v1/exams/" + f.examId() + "/submissions?status=PROCESSED", 200).get("totalElements").asInt()).isEqualTo(2);

        assertError(put(f.teacher(), "/api/v1/exams/" + f.examId() + "/questions", Map.of("questions", questions(N)), 409), 409, "EXAM_HAS_SUBMISSIONS");
        assertError(call("DELETE", f.teacher(), "/api/v1/exams/" + f.examId(), null, 409), 409, "EXAM_HAS_SUBMISSIONS");
    }

    @Test
    void originalPhotoCanBeDownloadedOnlyByTheOwner() {
        ExamFixture f = fixture();
        byte[] photo = SheetImages.jpeg(answeredSheet(f, f.students().get(0), correctChoices()));
        JsonNode result = parse(upload(f.teacher(), "/api/v1/exams/" + f.examId() + "/submissions", "image", "photo.jpg", JPEG,
                photo, Map.of()), 201);
        String url = "/api/v1/exams/" + f.examId() + "/submissions/" + result.get("id").asLong() + "/image";

        MvcResult image = download(f.teacher(), url);
        assertThat(image.getResponse().getStatus()).isEqualTo(200);
        assertThat(image.getResponse().getContentType()).isEqualTo("image/jpeg");
        assertThat(image.getResponse().getContentAsByteArray()).isEqualTo(photo);
        assertError(get(newTeacher(), url, 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(f.teacher(), "/api/v1/exams/" + f.examId() + "/submissions/999999/image", 404), 404, "RESOURCE_NOT_FOUND");
    }

    @Test
    void periodGradeIncludesTheProcessedExamScores() {
        ExamFixture f = fixture();
        submitOk(f, f.students().get(0), correctExcept(1, 2)); // 8/10 => logro 0.8 en EXAMS (60%)
        JsonNode report = get(f.teacher(), "/api/v1/teaching-periods/" + f.context().teachingPeriodId() + "/period-grades", 200);
        java.math.BigDecimal graded = null;
        java.math.BigDecimal notSubmitted = null;
        for (JsonNode s : report.get("students")) {
            if (s.get("studentId").asLong() == f.students().get(0).id()) {
                graded = s.get("periodGrade").decimalValue();
            }
            if (s.get("studentId").asLong() == f.students().get(1).id()) {
                notSubmitted = s.get("periodGrade").decimalValue();
            }
        }
        assertThat(graded).isEqualByComparingTo("2.40");      // 0.6 × 0.8 × 5
        assertThat(notSubmitted).isEqualByComparingTo("0.00");
    }

    @Test
    void processingRequiresAGradingConfigurationAndAnOwner() {
        ExamFixture f = fixture();
        // otro profesor no puede ni leer ni subir
        Teacher other = newTeacher();
        BufferedImage sheet = answeredSheet(f, f.students().get(0), correctChoices());
        assertError(parse(submit(other, f.examId(), sheet, Map.of()), 404), 404, "RESOURCE_NOT_FOUND");
        assertError(get(other, "/api/v1/exams/" + f.examId() + "/submissions", 404), 404, "RESOURCE_NOT_FOUND");

        // periodo sin configuración de calificación
        long period2 = post(f.teacher(), "/api/v1/academic-periods", Map.of("name", unique("P2"), "startDate", YEAR + "-05-01",
                "endDate", YEAR + "-07-01"), 201).get("id").asLong();
        long tp2 = post(f.teacher(), "/api/v1/teaching-periods", Map.of("teachingAssignmentId", f.context().assignmentId(),
                "academicPeriodId", period2), 201).get("id").asLong();
        Context c2 = new Context(f.context().gradeId(), f.context().groupId(), f.context().subjectId(), f.context().assignmentId(),
                period2, tp2, f.context().gradeName(), f.context().groupName());
        long exam2 = createExam(f.teacher(), c2, "Sin escala");
        BufferedImage sheet2 = SheetImages.render(sheetPdf(f.teacher(), exam2, f.students().get(0)), 0, DPI);
        assertError(parse(submit(f.teacher(), exam2, sheet2, Map.of()), 422), 422, "GRADING_CONFIGURATION_REQUIRED");
    }
}
