package com.edusistem.core.grading.domain.outputports;

import com.edusistem.core.grading.domain.entity.GradingScale;
import java.util.List;
import java.util.Optional;

public interface GradingScaleRepositoryPort {

    GradingScale save(GradingScale scale);

    Optional<GradingScale> findById(Long id);

    List<GradingScale> findAll();
}
