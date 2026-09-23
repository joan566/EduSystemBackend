package com.edusistem.core.imports.domain.vo;

public record ImportRowError(int rowNumber, String column, String message) {
}
