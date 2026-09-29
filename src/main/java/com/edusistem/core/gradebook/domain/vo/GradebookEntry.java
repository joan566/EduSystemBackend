package com.edusistem.core.gradebook.domain.vo;

import java.math.BigDecimal;

/**
 * Una evaluación en el reporte de un estudiante. {@code weight} es el peso efectivo en la nota final (en %): el peso
 * de su categoría repartido según el puntaje máximo de cada evaluación (así calcula el periodo). {@code contribution}
 * son los puntos (sobre 100) que aporta a la nota final. Ambos nulos si la clase no tiene configuración completa;
 * {@code weight} = 0 y {@code contribution} nulo si la evaluación no cuenta.
 */
public record GradebookEntry(GradebookRow row, String categoryName, BigDecimal weight, BigDecimal contribution,
                             boolean hasRubric, boolean hasAttachment) {
}
