package com.edusistem.core.imports.application.schoolsetup.sheets;

import com.edusistem.core.imports.application.contracts.ClassResolver;
import com.edusistem.core.imports.application.contracts.StudentRoster;
import com.edusistem.core.imports.application.schoolsetup.ClassKey;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import java.util.List;
import java.util.Optional;

/** Hojas cuyas filas pertenecen a una clase ya existente (identificada por las columnas de la clase). */
abstract class ClassBoundSheetImporter extends SheetImporterBase {

    private final ClassResolver classes;

    protected ClassBoundSheetImporter(String sheetName, List<String> requiredColumns, ClassResolver classes) {
        super(sheetName, requiredColumns);
        this.classes = classes;
    }

    /** Teaching period de la clase de la fila; si no existe registra el error y devuelve null. */
    protected final Long teachingPeriod(SchoolSetupImportContext context, RowReader row, ClassKey key) {
        Optional<Long> known = context.knownTeachingPeriod(key);
        if (known.isPresent()) {
            return known.get();
        }
        Optional<Long> found = classes.find(context.teacherId(), key);
        if (found.isEmpty()) {
            row.error("class", ImportMessages.classNotFound(key.gradeName(), key.groupName(), key.academicYear(),
                    key.subjectName(), key.academicPeriodName(), label(SchoolSetupSheets.CLASSES)));
            return null;
        }
        context.rememberTeachingPeriod(key, found.get());
        return found.get();
    }

    /** Estudiante activo en el grupo de la clase; si no está registra el error y devuelve null. */
    protected static Long enrolledStudent(SchoolSetupImportContext context, RowReader row, Long teachingPeriodId,
                                          String identification, StudentRoster roster) {
        Long studentId = context.roster(teachingPeriodId, roster).get(identification);
        if (studentId == null) {
            row.error("identification_number", ImportMessages.STUDENT_NOT_IN_CLASS_GROUP);
        }
        return studentId;
    }
}
