package com.edusistem.core.imports.application.student;

import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary.SheetSpec;
import java.util.List;
import java.util.Map;

/** Formato del Excel de importación de estudiantes, compartido por la plantilla y el procesador. */
public final class StudentImportSheet {

    public static final List<String> REQUIRED_COLUMNS =
            List.of("identification_number", "first_name", "last_name", "grade", "group");
    public static final List<String> TEMPLATE_COLUMNS = List.of("identification_number", "first_name", "last_name",
            "email", "grade", "group", "academic_year");
    /** Encabezados en español; al leer también se aceptan las claves en inglés de las plantillas antiguas. */
    public static final SheetSpec SHEET = new SheetSpec("Students", "Estudiantes", Map.of(
            "identification_number", "Número de identificación",
            "student_code", "Código",
            "first_name", "Nombres",
            "last_name", "Apellidos",
            "email", "Correo electrónico",
            "grade", "Grado",
            "group", "Grupo",
            "academic_year", "Año lectivo"));

    private StudentImportSheet() {
    }
}
