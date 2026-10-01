package com.edusistem.core.imports.application.student;

/** Fila validada y resuelta contra la base de datos, lista para aplicarse. */
public record StudentImportRow(int rowNumber, Long existingStudentId, String identificationNumber,
                        String studentCode, String firstName, String lastName, String email, Long groupId) {
}
