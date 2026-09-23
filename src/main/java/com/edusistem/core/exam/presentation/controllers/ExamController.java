package com.edusistem.core.exam.presentation.controllers;

import com.edusistem.core.exam.application.use_case.dtos.ExamCommands;
import com.edusistem.core.exam.domain.inputports.ManageExamUseCase;
import com.edusistem.core.exam.presentation.dtos.ExamDtos.CreateExamRequest;
import com.edusistem.core.exam.presentation.dtos.ExamDtos.ExamResponse;
import com.edusistem.core.exam.presentation.dtos.ExamDtos.ExamSummaryResponse;
import com.edusistem.core.exam.presentation.dtos.ExamDtos.QuestionRequest;
import com.edusistem.core.exam.presentation.dtos.ExamDtos.ReplaceQuestionsRequest;
import com.edusistem.core.exam.presentation.dtos.ExamDtos.UpdateExamRequest;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.shared.presentation.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "Exams")
public class ExamController {

    private final ManageExamUseCase exams;

    public ExamController(ManageExamUseCase exams) {
        this.exams = exams;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un examen (evaluación de categoría EXAMS) con preguntas opcionales")
    public ExamResponse create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateExamRequest request) {
        List<ExamCommands.QuestionInput> questions = request.questions() == null ? null
                : request.questions().stream().map(QuestionRequest::toInput).toList();
        return ExamResponse.from(exams.create(new ExamCommands.Create(user.id(), request.teachingPeriodId(),
                request.name(), request.description(), request.evaluationDate(), request.maximumScore(),
                request.numberOfQuestions(), questions)));
    }

    @GetMapping
    @Operation(summary = "Lista paginada de exámenes de un teaching period propio")
    public PageResponse<ExamSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @RequestParam Long teachingPeriodId,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        return PageResponse.from(exams.search(user.id(), teachingPeriodId, PageQuery.of(page, size)),
                ExamSummaryResponse::from);
    }

    @GetMapping("/{examId}")
    @Operation(summary = "Consulta un examen propio con sus preguntas, opciones y respuestas correctas")
    public ExamResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId) {
        return ExamResponse.from(exams.get(user.id(), examId));
    }

    @PutMapping("/{examId}")
    @Operation(summary = "Actualiza nombre, descripción y fecha del examen")
    public ExamResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId,
                               @Valid @RequestBody UpdateExamRequest request) {
        return ExamResponse.from(exams.update(new ExamCommands.Update(user.id(), examId, request.name(),
                request.description(), request.evaluationDate())));
    }

    @PutMapping("/{examId}/questions")
    @Operation(summary = "Reemplaza todas las preguntas del examen (no permitido si ya hay hojas procesadas)")
    public ExamResponse replaceQuestions(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId,
                                         @Valid @RequestBody ReplaceQuestionsRequest request) {
        return ExamResponse.from(exams.replaceQuestions(new ExamCommands.ReplaceQuestions(user.id(), examId,
                request.questions().stream().map(QuestionRequest::toInput).toList())));
    }

    @DeleteMapping("/{examId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un examen sin submissions")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long examId) {
        exams.delete(user.id(), examId);
    }
}
