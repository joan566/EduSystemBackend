package com.edusistem.core.imports.application.schoolsetup;

import com.edusistem.core.imports.application.support.RowReader;

/** Identifica una clase por nombres, como la referencian las hojas que dependen de ella. */
public record ClassKey(String gradeName, String groupName, int academicYear, String subjectName,
                       String academicPeriodName) {

    /** Lee las 5 columnas de la clase; null si alguna es inválida (los errores quedan registrados). */
    public static ClassKey read(RowReader row) {
        String gradeName = row.required("grade_name", 50);
        String groupName = row.required("group_name", 50);
        Integer academicYear = row.requiredYear("academic_year");
        String subjectName = row.required("subject_name", 100);
        String academicPeriodName = row.required("academic_period_name", 100);
        if (gradeName == null || groupName == null || academicYear == null || subjectName == null
                || academicPeriodName == null) {
            return null;
        }
        return new ClassKey(gradeName, groupName, academicYear, subjectName, academicPeriodName);
    }
}
