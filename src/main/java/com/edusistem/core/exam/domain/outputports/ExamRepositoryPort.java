package com.edusistem.core.exam.domain.outputports;

import com.edusistem.core.exam.domain.entity.Exam;
import com.edusistem.core.exam.domain.vo.ExamView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.Optional;

public interface ExamRepositoryPort {

    /** Guarda el examen y, si {@code questions} no está vacío, reemplaza sus preguntas y opciones. */
    Exam save(Exam exam);

    /** Siempre con preguntas y opciones cargadas. */
    Optional<Exam> findById(Long id);

    Optional<ExamView> findViewById(Long id);

    PageResult<ExamView> findViewsByTeachingPeriodId(Long teachingPeriodId, PageQuery page);

    boolean hasSubmissions(Long examId);

    void deleteById(Long id);
}
