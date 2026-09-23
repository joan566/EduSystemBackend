package com.edusistem.core.grading.domain.inputports;

import com.edusistem.core.grading.application.use_case.dtos.GradingCommands;
import com.edusistem.core.grading.domain.vo.GradingConfigurationView;

public interface ConfigureGradingUseCase {

    GradingConfigurationView save(GradingCommands.SaveConfiguration command);

    GradingConfigurationView get(Long teacherId, Long teachingPeriodId);
}
