package com.edusistem.core.exam.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Umbrales configurables de la lectura de hojas (edusistem.omr.*). */
@ConfigurationProperties(prefix = "edusistem.omr")
public record OmrProperties(Double reviewConfidenceThreshold, Double markThreshold, Double ambiguityThreshold) {
}
