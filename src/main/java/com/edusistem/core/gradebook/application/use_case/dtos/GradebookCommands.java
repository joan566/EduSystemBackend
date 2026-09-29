package com.edusistem.core.gradebook.application.use_case.dtos;

import java.math.BigDecimal;
import java.util.List;

public final class GradebookCommands {

    private GradebookCommands() {
    }

    /** {@code id} nulo para un criterio nuevo; los criterios existentes que no se envían se borran. */
    public record CriterionInput(Long id, String name, BigDecimal weight) {
    }

    public record SaveRubric(Long teacherId, Long evaluationId, List<CriterionInput> criteria) {
    }

    public record ScoreInput(Long criterionId, BigDecimal score) {
    }

    /** Califica con la rúbrica: debe traer un puntaje por criterio; la nota de la actividad se calcula. */
    public record ScoreWithRubric(Long teacherId, Long evaluationId, Long studentId, List<ScoreInput> scores,
                                  String comment) {
    }

    public record UploadAttachment(Long teacherId, Long evaluationId, Long studentId, String fileName,
                                   String contentType, byte[] content) {
    }

    /** Texto en blanco borra la observación. */
    public record SaveObservation(Long teacherId, Long teachingPeriodId, Long studentId, String text) {
    }
}
