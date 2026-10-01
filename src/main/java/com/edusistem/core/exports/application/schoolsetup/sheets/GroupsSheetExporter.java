package com.edusistem.core.exports.application.schoolsetup.sheets;

import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.TabularData;
import java.util.Comparator;

public class GroupsSheetExporter extends SheetExporterBase {

    public GroupsSheetExporter() {
        super(SchoolSetupSheets.GROUPS);
    }

    @Override
    public TabularData export(SchoolSetupExportContext context) {
        return table(context.groups().stream()
                .sorted(Comparator.comparing(GroupView::academicYear).thenComparing(GroupView::gradeName)
                        .thenComparing(GroupView::name))
                .map(g -> values(g.gradeName(), g.name(), g.academicYear())).toList());
    }
}
