package com.edusistem.core.student.domain.outputports;

public interface StudentCodeGeneratorPort {

    /** Genera un student_code único (secuencia de base de datos). */
    String nextStudentCode();
}
