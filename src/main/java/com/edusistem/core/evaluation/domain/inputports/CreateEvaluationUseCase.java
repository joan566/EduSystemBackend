package com.edusistem.core.evaluation.domain.inputports;

import com.edusistem.core.evaluation.application.use_case.dtos.EvaluationCommands;
import com.edusistem.core.evaluation.domain.entity.Evaluation;

/** Crea la evaluación base de un examen, actividad o sesión de asistencia (lo invocan esos módulos). */
public interface CreateEvaluationUseCase {

    Evaluation create(EvaluationCommands.Create command);
}
