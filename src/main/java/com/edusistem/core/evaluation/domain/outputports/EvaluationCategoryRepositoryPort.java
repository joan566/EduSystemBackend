package com.edusistem.core.evaluation.domain.outputports;

import com.edusistem.core.evaluation.domain.entity.EvaluationCategory;
import java.util.List;
import java.util.Optional;

public interface EvaluationCategoryRepositoryPort {

    List<EvaluationCategory> findAll();

    Optional<EvaluationCategory> findById(Long id);

    Optional<EvaluationCategory> findByName(String name);
}
