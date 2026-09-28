package com.edusistem.core.exam.presentation.controllers;

import com.edusistem.core.exam.application.use_case.dtos.ExamCommands;
import com.edusistem.core.exam.domain.inputports.ImportExamFromDocumentUseCase;
import com.edusistem.core.exam.presentation.dtos.ExamDtos.ExamResponse;
import com.edusistem.core.exam.presentation.dtos.ExamDtos.QuestionsPreviewResponse;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "Exams")
public class ExamDocumentController {

    static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String FORMAT = "Formato: \"1. Enunciado\", opciones \"A) Texto\", \"Respuesta: B\" y, opcional, "
            + "\"Puntos: 2\". También acepta la opción correcta marcada con * y las listas automáticas de Word. "
            + "Lo que haya antes de la pregunta 1 se ignora. Si hay errores responde 400 INVALID_QUESTION_DOCUMENT "
            + "con la lista de problemas en errors.";
    private static final int MAX_NAME_LENGTH = 150;

    private final ImportExamFromDocumentUseCase documents;

    public ExamDocumentController(ImportExamFromDocumentUseCase documents) {
        this.documents = documents;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un examen con las preguntas de un documento Word (.docx)",
            description = FORMAT + " Si no se indica name se usa el nombre del archivo.")
    public ExamResponse importExam(@AuthenticationPrincipal AuthenticatedUser user,
                                   @RequestPart("file") MultipartFile file,
                                   @RequestParam Long teachingPeriodId,
                                   @RequestParam(required = false) String name,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime evaluationDate,
                                   @RequestParam(required = false) BigDecimal maximumScore) throws IOException {
        String examName = name != null && !name.isBlank() ? name.strip() : nameFromFile(file.getOriginalFilename());
        if (examName.length() > MAX_NAME_LENGTH) {
            throw new InvalidRequestException("INVALID_NAME", "name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        if (maximumScore != null && maximumScore.signum() <= 0) {
            throw new InvalidRequestException("INVALID_MAXIMUM_SCORE", "maximumScore must be greater than zero");
        }
        return ExamResponse.from(documents.createFromDocument(new ExamCommands.CreateFromDocument(user.id(),
                teachingPeriodId, examName, description, evaluationDate, maximumScore, content(file))));
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Interpreta un documento Word sin guardar nada, para revisar las preguntas antes de importarlas",
            description = FORMAT)
    public QuestionsPreviewResponse preview(@RequestPart("file") MultipartFile file) throws IOException {
        return QuestionsPreviewResponse.from(documents.preview(content(file)));
    }

    @PostMapping(value = "/{examId}/questions/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Reemplaza las preguntas de un examen con las de un documento Word (.docx)",
            description = FORMAT + " No se permite si ya hay hojas procesadas.")
    public ExamResponse replaceQuestions(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId,
                                         @RequestPart("file") MultipartFile file) throws IOException {
        return ExamResponse.from(documents.replaceQuestionsFromDocument(
                new ExamCommands.ReplaceQuestionsFromDocument(user.id(), examId, content(file))));
    }

    @GetMapping("/import/template")
    @Operation(summary = "Descarga la plantilla Word (.docx) con el formato de preguntas que entiende la importación")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("questions-template.docx").build().toString())
                .contentType(MediaType.parseMediaType(DOCX))
                .body(documents.template());
    }

    private static byte[] content(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidRequestException("EMPTY_FILE", "The uploaded file is empty");
        }
        return file.getBytes();
    }

    private static String nameFromFile(String fileName) {
        String base = fileName == null ? "" : fileName.replaceAll("^.*[/\\\\]", "").replaceFirst("\\.[^.]*$", "").strip();
        if (base.isEmpty()) {
            throw new InvalidRequestException("NAME_REQUIRED", "name is required");
        }
        return base.length() > MAX_NAME_LENGTH ? base.substring(0, MAX_NAME_LENGTH).strip() : base;
    }
}
