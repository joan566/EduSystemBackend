package com.edusistem.core.gradebook.presentation.controllers;

import com.edusistem.core.gradebook.application.use_case.dtos.GradebookCommands;
import com.edusistem.core.gradebook.domain.inputports.ManageGradeAttachmentUseCase;
import com.edusistem.core.gradebook.domain.inputports.ManageRubricUseCase;
import com.edusistem.core.gradebook.domain.inputports.ManageStudentObservationUseCase;
import com.edusistem.core.gradebook.domain.inputports.QueryGradebookUseCase;
import com.edusistem.core.gradebook.presentation.dtos.GradebookDtos;
import com.edusistem.core.gradebook.presentation.dtos.GradebookDtos.AttachmentResponse;
import com.edusistem.core.gradebook.presentation.dtos.GradebookDtos.CriterionResponse;
import com.edusistem.core.gradebook.presentation.dtos.GradebookDtos.GradeDetailResponse;
import com.edusistem.core.gradebook.presentation.dtos.GradebookDtos.ObservationRequest;
import com.edusistem.core.gradebook.presentation.dtos.GradebookDtos.ObservationResponse;
import com.edusistem.core.gradebook.presentation.dtos.GradebookDtos.RubricRequest;
import com.edusistem.core.gradebook.presentation.dtos.GradebookDtos.RubricScoresRequest;
import com.edusistem.core.gradebook.presentation.dtos.GradebookDtos.StudentGradeReportResponse;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Gradebook")
public class GradebookController {

    private final QueryGradebookUseCase gradebook;
    private final ManageRubricUseCase rubrics;
    private final ManageGradeAttachmentUseCase attachments;
    private final ManageStudentObservationUseCase observations;

    public GradebookController(QueryGradebookUseCase gradebook, ManageRubricUseCase rubrics,
                               ManageGradeAttachmentUseCase attachments, ManageStudentObservationUseCase observations) {
        this.gradebook = gradebook;
        this.rubrics = rubrics;
        this.attachments = attachments;
        this.observations = observations;
    }

    @GetMapping("/teaching-periods/{teachingPeriodId}/students/{studentId}/grade-report")
    @Operation(summary = "Notas de un estudiante en la clase, evaluación por evaluación (peso efectivo y aporte)")
    public StudentGradeReportResponse report(@AuthenticationPrincipal AuthenticatedUser user,
                                             @PathVariable Long teachingPeriodId, @PathVariable Long studentId) {
        return StudentGradeReportResponse.from(gradebook.studentReport(user.id(), teachingPeriodId, studentId));
    }

    @PutMapping("/teaching-periods/{teachingPeriodId}/students/{studentId}/observation")
    @Operation(summary = "Guarda la observación del docente sobre el estudiante (texto en blanco la borra: 204)")
    public ResponseEntity<ObservationResponse> saveObservation(@AuthenticationPrincipal AuthenticatedUser user,
                                                               @PathVariable Long teachingPeriodId,
                                                               @PathVariable Long studentId,
                                                               @Valid @RequestBody ObservationRequest request) {
        var saved = observations.save(new GradebookCommands.SaveObservation(user.id(), teachingPeriodId, studentId,
                request.text()));
        return saved == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(ObservationResponse.from(saved));
    }

    @GetMapping("/evaluations/{evaluationId}/students/{studentId}/grade-detail")
    @Operation(summary = "La nota de un estudiante en una evaluación, con su rúbrica y archivo adjunto")
    public GradeDetailResponse detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long evaluationId,
                                      @PathVariable Long studentId) {
        return GradeDetailResponse.from(gradebook.gradeDetail(user.id(), evaluationId, studentId));
    }

    @GetMapping("/evaluations/{evaluationId}/rubric")
    @Operation(summary = "Criterios de la rúbrica de una actividad")
    public List<CriterionResponse> rubric(@AuthenticationPrincipal AuthenticatedUser user,
                                          @PathVariable Long evaluationId) {
        return GradebookDtos.criteria(rubrics.get(user.id(), evaluationId));
    }

    @PutMapping("/evaluations/{evaluationId}/rubric")
    @Operation(summary = "Define la rúbrica de una actividad (pesos que suman 100; criterios sin id son nuevos, "
            + "los existentes que no se envían se borran con sus puntajes)")
    public List<CriterionResponse> saveRubric(@AuthenticationPrincipal AuthenticatedUser user,
                                              @PathVariable Long evaluationId,
                                              @Valid @RequestBody RubricRequest request) {
        return GradebookDtos.criteria(rubrics.save(new GradebookCommands.SaveRubric(user.id(), evaluationId,
                request.toInputs())));
    }

    @DeleteMapping("/evaluations/{evaluationId}/rubric")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quita la rúbrica de una actividad (las notas ya registradas se conservan)")
    public void deleteRubric(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long evaluationId) {
        rubrics.delete(user.id(), evaluationId);
    }

    @PutMapping("/evaluations/{evaluationId}/students/{studentId}/rubric-scores")
    @Operation(summary = "Califica con la rúbrica: un puntaje por criterio; la nota de la actividad es Σ puntaje × peso")
    public GradeDetailResponse score(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long evaluationId,
                                     @PathVariable Long studentId, @Valid @RequestBody RubricScoresRequest request) {
        return GradeDetailResponse.from(rubrics.score(new GradebookCommands.ScoreWithRubric(user.id(), evaluationId,
                studentId, request.toInputs(), request.comment())));
    }

    @PostMapping(value = "/evaluations/{evaluationId}/students/{studentId}/attachment",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Adjunta un archivo a la nota (máx. 10 MB; reemplaza el anterior)")
    public AttachmentResponse upload(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long evaluationId,
                                     @PathVariable Long studentId, @RequestPart("file") MultipartFile file)
            throws IOException {
        return AttachmentResponse.from(attachments.upload(new GradebookCommands.UploadAttachment(user.id(),
                evaluationId, studentId, file.getOriginalFilename(), file.getContentType(), file.getBytes())));
    }

    @GetMapping("/evaluations/{evaluationId}/students/{studentId}/attachment")
    @Operation(summary = "Descarga el archivo adjunto de la nota")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal AuthenticatedUser user,
                                           @PathVariable Long evaluationId, @PathVariable Long studentId) {
        var file = attachments.download(user.id(), evaluationId, studentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName(), java.nio.charset.StandardCharsets.UTF_8)
                                .build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    @DeleteMapping("/evaluations/{evaluationId}/students/{studentId}/attachment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quita el archivo adjunto de la nota")
    public void deleteAttachment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long evaluationId,
                                 @PathVariable Long studentId) {
        attachments.delete(user.id(), evaluationId, studentId);
    }
}
