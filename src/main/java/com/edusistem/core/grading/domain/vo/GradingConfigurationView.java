package com.edusistem.core.grading.domain.vo;

import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import com.edusistem.core.grading.domain.entity.GradingScale;

public record GradingConfigurationView(GradingConfiguration configuration, GradingScale scale) {
}
