package com.edusistem.core.exam.domain.vo;

/** Datos impresos en una hoja de respuestas. */
public record AnswerSheetData(String examName, String subjectName, String groupName, String studentName,
                              String studentCode, String qrContent, AnswerSheetLayout layout) {
}
