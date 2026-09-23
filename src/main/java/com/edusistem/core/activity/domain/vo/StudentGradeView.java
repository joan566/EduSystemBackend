package com.edusistem.core.activity.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Nota de un estudiante activo del grupo; {@code grade} es nulo si aún no fue calificado. */
public record StudentGradeView(Long studentId, String studentCode, String studentName, BigDecimal grade,
                               String comment, LocalDateTime gradedAt) {
}
