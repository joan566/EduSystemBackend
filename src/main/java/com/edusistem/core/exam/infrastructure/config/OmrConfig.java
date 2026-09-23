package com.edusistem.core.exam.infrastructure.config;

import com.edusistem.core.exam.domain.service.BubbleClassifier;
import com.edusistem.core.exam.domain.vo.OmrThresholds;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OmrConfig {

    @Bean
    OmrThresholds omrThresholds(OmrProperties properties) {
        OmrThresholds defaults = OmrThresholds.defaults();
        return new OmrThresholds(
                properties.markThreshold() != null ? properties.markThreshold() : defaults.markThreshold(),
                properties.ambiguityThreshold() != null ? properties.ambiguityThreshold() : defaults.ambiguityThreshold(),
                properties.reviewConfidenceThreshold() != null ? properties.reviewConfidenceThreshold()
                        : defaults.reviewConfidence());
    }

    @Bean
    BubbleClassifier bubbleClassifier(OmrThresholds thresholds) {
        return new BubbleClassifier(thresholds);
    }
}
