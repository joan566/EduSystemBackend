package com.edusistem.core.exports.application.schoolsetup.sheets;

import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.TabularData;
import java.util.Comparator;

public class AcademicPeriodsSheetExporter extends SheetExporterBase {

    private final AcademicPeriodRepositoryPort academicPeriods;

    public AcademicPeriodsSheetExporter(AcademicPeriodRepositoryPort academicPeriods) {
        super(SchoolSetupSheets.ACADEMIC_PERIODS);
        this.academicPeriods = academicPeriods;
    }

    @Override
    public TabularData export(SchoolSetupExportContext context) {
        return table(PageResult.collectAll(page -> academicPeriods.findByTeacherId(context.teacherId(), page)).stream()
                .sorted(Comparator.comparing(AcademicPeriod::getStartDate).thenComparing(AcademicPeriod::getName))
                .map(p -> values(p.getName(), p.getStartDate(), p.getEndDate())).toList());
    }
}
