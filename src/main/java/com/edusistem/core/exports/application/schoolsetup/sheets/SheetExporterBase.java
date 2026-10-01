package com.edusistem.core.exports.application.schoolsetup.sheets;

import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.exports.application.contracts.SchoolSetupSheetExporter;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.TabularData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Nombre de la hoja y ayudantes para armar filas con las columnas de {@link SchoolSetupSheets}. */
abstract class SheetExporterBase implements SchoolSetupSheetExporter {

    private final String sheetName;

    protected SheetExporterBase(String sheetName) {
        this.sheetName = sheetName;
    }

    @Override
    public final String sheetName() {
        return sheetName;
    }

    protected final TabularData table(List<List<Object>> rows) {
        return SchoolSetupSheets.table(sheetName, rows, List.of());
    }

    /** Fila de una hoja que depende de una clase: las 5 columnas que la identifican y luego {@code values}. */
    protected static List<Object> row(TeachingPeriodView tp, Object... values) {
        List<Object> row = new ArrayList<>(List.of(tp.gradeName(), tp.groupName(), tp.academicYear(), tp.subjectName(),
                tp.academicPeriodName()));
        row.addAll(Arrays.asList(values));
        return row;
    }

    /** Como {@code List.of} pero admite celdas vacías (null). */
    protected static List<Object> values(Object... values) {
        return Arrays.asList(values);
    }
}
