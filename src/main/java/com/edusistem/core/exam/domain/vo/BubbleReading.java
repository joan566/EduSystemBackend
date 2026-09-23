package com.edusistem.core.exam.domain.vo;

/** Fracción de relleno (0..1) medida por el lector para cada burbuja: {@code fillRatios[pregunta-1][opción]}. */
public record BubbleReading(double[][] fillRatios) {
}
