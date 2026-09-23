package com.edusistem.core.evaluation.presentation.controllers;

import com.edusistem.core.evaluation.application.use_case.dtos.EvaluationCommands;
import com.edusistem.core.evaluation.domain.inputports.QueryEvaluationUseCase;
import com.edusistem.core.evaluation.presentation.dtos.EvaluationDtos.CategoryResponse;
import com.edusistem.core.evaluation.presentation.dtos.EvaluationDtos.EvaluationResponse;
import com.edusistem.core.evaluation.presentation.dtos.EvaluationDtos.UpdateRequest;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.shared.presentation.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Las evaluaciones se crean desde /exams, /activities y /attendance-sessions; aquí se consultan de forma unificada. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Evaluations")
public class EvaluationController {

    private final QueryEvaluationUseCase evaluations;

    public EvaluationController(QueryEvaluationUseCase evaluations) {
        this.evaluations = evaluations;
    }

    @GetMapping("/evaluation-categories")
    @Operation(summary = "Lista las categorías de evaluación (EXAMS, ACTIVITIES, ATTENDANCE)")
    public List<CategoryResponse> categories() {
        return evaluations.listCategories().stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/evaluations")
    @Operation(summary = "Lista paginada de evaluaciones de un teaching period propio")
    public PageResponse<EvaluationResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @RequestParam Long teachingPeriodId,
                                                 @RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size) {
        return PageResponse.from(evaluations.search(user.id(), teachingPeriodId, categoryId, PageQuery.of(page, size)),
                EvaluationResponse::from);
    }

    @GetMapping("/evaluations/{id}")
    @Operation(summary = "Consulta una evaluación propia")
    public EvaluationResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return EvaluationResponse.from(evaluations.get(user.id(), id));
    }

    @PutMapping("/evaluations/{id}")
    @Operation(summary = "Actualiza nombre, descripción y fecha de una evaluación")
    public EvaluationResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                     @Valid @RequestBody UpdateRequest request) {
        return EvaluationResponse.from(evaluations.updateDetails(new EvaluationCommands.UpdateDetails(user.id(), id,
                request.name(), request.description(), request.evaluationDate())));
    }
}
