package com.edusistem.core.evaluation.domain.outputports;

import com.edusistem.core.evaluation.domain.entity.Evaluation;
import com.edusistem.core.evaluation.domain.vo.EvaluationView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.List;
import java.util.Optional;

public interface EvaluationRepositoryPort {

    Evaluation save(Evaluation evaluation);

    Optional<Evaluation> findById(Long id);

    Optional<EvaluationView> findViewById(Long id);

    PageResult<EvaluationView> findViewsByTeachingPeriodId(Long teachingPeriodId, Long categoryId, PageQuery page);

    List<Evaluation> findByTeachingPeriodId(Long teachingPeriodId);

    void deleteById(Long id);
}
