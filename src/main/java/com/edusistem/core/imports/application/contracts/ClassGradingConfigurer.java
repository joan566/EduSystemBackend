package com.edusistem.core.imports.application.contracts;

import com.edusistem.core.evaluation.domain.enums.EvaluationCategoryCode;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.imports.application.support.RowReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Configuración de notas de una clase (escala, nota mínima y pesos) que llega en las columnas opcionales de Clases. */
public interface ClassGradingConfigurer {

    /** Configuración pedida en una fila; {@code scale} y cada peso son nulos si la celda está vacía. */
    record GradingInput(GradingScale scale, BigDecimal passingGrade, Map<EvaluationCategoryCode, BigDecimal> weights) {

        public boolean hasWeights() {
            return !weights.isEmpty();
        }
    }

    /** Escalas para la lista desplegable de la hoja Clases ({@code teacherId} nulo: sólo las del sistema). */
    List<String> scaleOptions(Long teacherId);

    /** Lee las columnas de configuración; vacío si la fila no trae ninguna. Las celdas inválidas quedan como errores. */
    Optional<GradingInput> read(Long teacherId, RowReader row);

    /** Guarda la configuración de la clase; devuelve false (con el error registrado) si no se pudo. */
    boolean apply(Long teacherId, RowReader row, Long teachingPeriodId, GradingInput input);
}
