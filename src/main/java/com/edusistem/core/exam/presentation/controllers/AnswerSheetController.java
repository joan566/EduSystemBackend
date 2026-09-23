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
    @Operation(summary = "Genera el PDF de la hoja de respuestas de un estudiante")
    public ResponseEntity<byte[]> forStudent(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId,
                                             @PathVariable Long studentId) {
        return pdf(answerSheets.forStudent(user.id(), examId, studentId));
    }

    @GetMapping(value = "/answer-sheets", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Genera un PDF con las hojas de todos los estudiantes activos del grupo")
    public ResponseEntity<byte[]> forGroup(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId) {
        return pdf(answerSheets.forGroup(user.id(), examId));
    }

    private static ResponseEntity<byte[]> pdf(PdfDocument document) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.fileName()).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(document.content());
    }
}
