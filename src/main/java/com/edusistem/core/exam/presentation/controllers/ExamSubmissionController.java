package com.edusistem.core.exam.presentation.controllers;

import com.edusistem.core.exam.application.use_case.dtos.SubmissionCommands;
import com.edusistem.core.exam.domain.enums.ExamSubmissionStatus;
import com.edusistem.core.exam.domain.inputports.QuerySubmissionUseCase;
import com.edusistem.core.exam.domain.inputports.ReviewSubmissionUseCase;
import com.edusistem.core.exam.domain.inputports.SubmitAnswerSheetUseCase;
import com.edusistem.core.exam.presentation.dtos.SubmissionDtos.SubmissionResponse;
import com.edusistem.core.exam.presentation.dtos.SubmissionDtos.SubmissionSummaryResponse;
import com.edusistem.core.exam.presentation.dtos.SubmissionDtos.UpdateAnswerRequest;
import com.edusistem.core.exam.presentation.dtos.SubmissionDtos.UpdateFinalGradeRequest;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.shared.presentation.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/exams/{examId}/submissions")
@Tag(name = "Exam submissions")
public class ExamSubmissionController {

    private final SubmitAnswerSheetUseCase submit;
    private final QuerySubmissionUseCase query;
    private final ReviewSubmissionUseCase review;

    public ExamSubmissionController(SubmitAnswerSheetUseCase submit, QuerySubmissionUseCase query,
                                    ReviewSubmissionUseCase review) {
        this.submit = submit;
        this.query = query;
        this.review = review;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sube y procesa la foto de una hoja de respuestas",
            description = "Lee el QR, identifica al estudiante, detecta las burbujas y calcula puntuación y nota. Si la imagen "
                    + "no puede leerse la submission queda en FAILED con su motivo. 'studentId' solo se usa si el QR no es legible; "
                    + "'replace=true' permite reprocesar a un estudiante que ya tiene resultado.")
    public SubmissionResponse upload(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId,
                                     @RequestPart("image") MultipartFile image,
                                     @RequestParam(required = false) Long studentId,
                                     @RequestParam(defaultValue = "false") boolean replace) throws IOException {
        if (image.isEmpty()) {
            throw new InvalidRequestException("INVALID_IMAGE", "The image is empty");
        }
        return SubmissionResponse.from(submit.submit(new SubmissionCommands.Submit(user.id(), examId, image.getBytes(),
                image.getOriginalFilename(), studentId, replace)));
    }

    @GetMapping
    @Operation(summary = "Lista paginada de submissions del examen (filtro opcional por estado)")
    public PageResponse<SubmissionSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @PathVariable Long examId,
                                                        @RequestParam(required = false) ExamSubmissionStatus status,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size) {
        return PageResponse.from(query.search(user.id(), examId, status, PageQuery.of(page, size)),
                SubmissionSummaryResponse::from);
    }

    @GetMapping("/{submissionId}")
    @Operation(summary = "Detalle de una submission: estudiante, respuestas detectadas, confianza y resultado")
    public SubmissionResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId,
                                  @PathVariable Long submissionId) {
        return SubmissionResponse.from(query.get(user.id(), examId, submissionId));
    }

    @GetMapping("/{submissionId}/image")
    @Operation(summary = "Descarga la foto original de la hoja subida (para revisar manualmente)")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId,
                                        @PathVariable Long submissionId) {
        var image = query.image(user.id(), examId, submissionId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore()).body(image.content());
    }

    @PutMapping("/{submissionId}/answers/{questionNumber}")
    @Operation(summary = "Corrige manualmente la respuesta de una pregunta (auditado); recalcula puntuación y nota")
    public SubmissionResponse updateAnswer(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId,
                                           @PathVariable Long submissionId, @PathVariable int questionNumber,
                                           @Valid @RequestBody UpdateAnswerRequest request) {
        return SubmissionResponse.from(review.updateAnswer(new SubmissionCommands.UpdateAnswer(user.id(), examId,
                submissionId, questionNumber, request.selectedOption(), request.reason())));
    }

    @PutMapping("/{submissionId}/final-grade")
    @Operation(summary = "Modifica manualmente la nota final dentro de la escala (auditado)")
    public SubmissionResponse updateFinalGrade(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId,
                                               @PathVariable Long submissionId,
                                               @Valid @RequestBody UpdateFinalGradeRequest request) {
        return SubmissionResponse.from(review.updateFinalGrade(new SubmissionCommands.UpdateFinalGrade(user.id(), examId,
                submissionId, request.finalGrade(), request.reason())));
    }
}
