package com.edusistem.core.exam.domain.vo;

/**
 * Umbrales de decisión configurables.
 * @param markThreshold fracción de relleno a partir de la cual una burbuja se considera marcada
 * @param ambiguityThreshold por encima de esto (y por debajo de markThreshold) la burbuja es dudosa
 * @param reviewConfidence confianza mínima para aceptar automáticamente una respuesta
 */
public record OmrThresholds(double markThreshold, double ambiguityThreshold, double reviewConfidence) {

    public static OmrThresholds defaults() {
        return new OmrThresholds(0.45, 0.20, 0.90);
    }
}
