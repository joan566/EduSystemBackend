package com.edusistem.core.grading.domain.outputports;

import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import java.util.Optional;

public interface GradingConfigurationRepositoryPort {

    /** Guarda la configuración y reemplaza por completo sus pesos. */
    GradingConfiguration save(GradingConfiguration configuration);

    Optional<GradingConfiguration> findByTeachingPeriodId(Long teachingPeriodId);
}
