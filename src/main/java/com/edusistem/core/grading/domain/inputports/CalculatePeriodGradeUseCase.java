package com.edusistem.core.grading.domain.inputports;

import com.edusistem.core.grading.domain.vo.PeriodGradeReport;

public interface CalculatePeriodGradeUseCase {

    /** Requiere configuración con pesos que sumen 100%. */
    PeriodGradeReport calculate(Long teacherId, Long teachingPeriodId);
}
