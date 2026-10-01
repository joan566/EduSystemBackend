package com.edusistem.core.imports.application.schoolsetup.sheets;

import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.academic.domain.entity.Group;
import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.imports.application.contracts.ClassGradingConfigurer;
import com.edusistem.core.imports.application.contracts.ClassGradingConfigurer.GradingInput;
import com.edusistem.core.imports.application.contracts.ClassResolver;
import com.edusistem.core.imports.application.schoolsetup.ClassKey;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.subject.domain.entity.Subject;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import java.util.List;
import java.util.Optional;

/**
 * Hoja Clases: busca o crea la asignación (grupo + asignatura) y el teaching period (+ periodo) de cada fila, y aplica
 * la configuración de notas si la fila la trae. Las clases quedan en el contexto para las hojas siguientes.
 */
public class ClassesSheetImporter extends SheetImporterBase {

    private final GroupRepositoryPort groups;
    private final SubjectRepositoryPort subjects;
    private final AcademicPeriodRepositoryPort academicPeriods;
    private final ClassResolver classes;
    private final ClassGradingConfigurer grading;

    public ClassesSheetImporter(GroupRepositoryPort groups, SubjectRepositoryPort subjects,
                                AcademicPeriodRepositoryPort academicPeriods, ClassResolver classes,
                                ClassGradingConfigurer grading) {
        super(SchoolSetupSheets.CLASSES, SchoolSetupSheets.CLASS_KEY_COLUMNS);
        this.groups = groups;
        this.subjects = subjects;
        this.academicPeriods = academicPeriods;
        this.classes = classes;
        this.grading = grading;
    }

    @Override
    protected void importRows(SchoolSetupImportContext context, List<SpreadsheetRow> rows) {
        Long teacherId = context.teacherId();
        for (SpreadsheetRow sheetRow : rows) {
            RowReader row = reader(context, sheetRow);
            int before = context.errors().size();
            ClassKey key = ClassKey.read(row);
            Optional<GradingInput> gradingInput = grading.read(teacherId, row);
            if (context.errors().size() > before || key == null) {
                markFailed(context, row);
                continue;
            }
            try {
                Optional<Group> group = groups.findByGradeNameAndNameAndAcademicYear(teacherId, key.gradeName(),
                        key.groupName(), key.academicYear());
                if (group.isEmpty()) {
                    row.error("group_name", ImportMessages.groupNotFound(key.groupName(), key.gradeName(),
                            key.academicYear(), label(SchoolSetupSheets.GROUPS)));
                    markFailed(context, row);
                    continue;
                }
                Optional<Subject> subject = subjects.findByTeacherIdAndName(teacherId, key.subjectName());
                if (subject.isEmpty()) {
                    row.error("subject_name",
                            ImportMessages.subjectNotFound(key.subjectName(), label(SchoolSetupSheets.SUBJECTS)));
                    markFailed(context, row);
                    continue;
                }
                Optional<AcademicPeriod> period = academicPeriods.findByTeacherIdAndName(teacherId,
                        key.academicPeriodName());
                if (period.isEmpty()) {
                    row.error("academic_period_name", ImportMessages.periodNotFound(key.academicPeriodName(),
                            label(SchoolSetupSheets.ACADEMIC_PERIODS)));
                    markFailed(context, row);
                    continue;
                }
                Long teachingPeriodId = classes.findOrCreate(teacherId, group.get().getId(), subject.get().getId(),
                        period.get().getId());
                context.rememberTeachingPeriod(key, teachingPeriodId);
                if (gradingInput.isPresent() && !grading.apply(teacherId, row, teachingPeriodId, gradingInput.get())) {
                    markFailed(context, row);
                }
            } catch (RuntimeException e) {
                fail(context, row, ImportMessages.couldNotCreate(e.getMessage()));
            }
        }
    }
}
