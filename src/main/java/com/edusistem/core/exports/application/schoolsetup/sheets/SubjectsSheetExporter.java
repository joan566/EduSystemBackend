package com.edusistem.core.exports.application.schoolsetup.sheets;

import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.TabularData;
import com.edusistem.core.subject.domain.entity.Subject;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import java.util.Comparator;

public class SubjectsSheetExporter extends SheetExporterBase {

    private final SubjectRepositoryPort subjects;

    public SubjectsSheetExporter(SubjectRepositoryPort subjects) {
        super(SchoolSetupSheets.SUBJECTS);
        this.subjects = subjects;
    }

    @Override
    public TabularData export(SchoolSetupExportContext context) {
        return table(PageResult.collectAll(page -> subjects.search(context.teacherId(), null, page)).stream()
                .sorted(Comparator.comparing(Subject::getName))
                .map(s -> values(s.getName(), s.getDescription())).toList());
    }
}
