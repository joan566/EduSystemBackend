package com.edusistem.core.imports.application.use_case.service;

/** Fila validada y resuelta contra la base de datos, lista para aplicarse. */
record StudentImportRow(int rowNumber, Long existingStudentId, String identificationNumber,
                        String studentCode, String firstName, String lastName, String email, Long groupId) {
}
