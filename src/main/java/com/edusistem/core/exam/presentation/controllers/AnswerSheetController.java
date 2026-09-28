package com.edusistem.core.exam.presentation.controllers;

import com.edusistem.core.exam.domain.inputports.GenerateAnswerSheetUseCase;
import com.edusistem.core.exam.domain.vo.PdfDocument;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exams/{examId}")
@Tag(name = "Answer sheets")
public class AnswerSheetController {

    private final GenerateAnswerSheetUseCase answerSheets;

    public AnswerSheetController(GenerateAnswerSheetUseCase answerSheets) {
        this.answerSheets = answerSheets;
    }

    @GetMapping(value = "/answer-sheet/{studentId}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Genera el PDF de la hoja de respuestas de un estudiante",
            description = "Con includeQuestions=true (por defecto) a la hoja le sigue el cuadernillo de preguntas.")
    public ResponseEntity<byte[]> forStudent(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId,
                                             @PathVariable Long studentId,
                                             @RequestParam(defaultValue = "true") boolean includeQuestions) {
        return pdf(answerSheets.forStudent(user.id(), examId, studentId, includeQuestions));
    }

    @GetMapping(value = "/answer-sheets", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Genera un PDF con las hojas de todos los estudiantes activos del grupo",
            description = "Con includeQuestions=true (por defecto) cada hoja va seguida del cuadernillo de preguntas, "
                    + "de modo que cada estudiante recibe su juego completo.")
    public ResponseEntity<byte[]> forGroup(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId,
                                           @RequestParam(defaultValue = "true") boolean includeQuestions) {
        return pdf(answerSheets.forGroup(user.id(), examId, includeQuestions));
    }

    @GetMapping(value = "/question-booklet", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Genera solo el cuadernillo de preguntas (enunciados y opciones, sin respuestas)")
    public ResponseEntity<byte[]> questionBooklet(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @PathVariable Long examId) {
        return pdf(answerSheets.questionBooklet(user.id(), examId));
    }

    private static ResponseEntity<byte[]> pdf(PdfDocument document) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.fileName()).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(document.content());
    }
}
