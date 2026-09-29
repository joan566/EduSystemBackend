package com.edusistem.core.gradebook.domain.vo;

import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.vo.CategoryBreakdown;
import com.edusistem.core.gradebook.domain.entity.StudentObservation;
import com.edusistem.core.student.domain.entity.Student;
import java.math.BigDecimal;
import java.util.List;

/**
 * Notas de un estudiante en un teaching period, evaluación por evaluación. {@code scale}, {@code periodGrade},
 * {@code score} (puntos sobre 100) y {@code passing} son nulos si la clase no tiene la configuración completa
 * ({@code passing} también si no define nota mínima).
 */
public record StudentGradeReport(Long teachingPeriodId, Student student, GradingScale scale, BigDecimal passingGrade,
                                 BigDecimal totalWeight, boolean configurationComplete, BigDecimal periodGrade,
                                 BigDecimal score, Boolean passing, List<CategoryBreakdown> categories,
                                 List<GradebookEntry> evaluations, StudentObservation observation) {
}
