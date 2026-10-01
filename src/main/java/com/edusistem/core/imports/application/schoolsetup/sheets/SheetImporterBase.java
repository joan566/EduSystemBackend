package com.edusistem.core.imports.application.schoolsetup.sheets;

import com.edusistem.core.imports.application.contracts.SchoolSetupSheetImporter;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.application.support.SheetColumns;
import com.edusistem.core.imports.domain.vo.ParsedSheet;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary.SheetSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pasos comunes a todas las hojas: la hoja es opcional, sus columnas obligatorias deben estar y luego cada importador
 * procesa sus filas.
 */
abstract class SheetImporterBase implements SchoolSetupSheetImporter {

    private final String sheetName;
    private final List<String> requiredColumns;

    protected SheetImporterBase(String sheetName, List<String> requiredColumns) {
        this.sheetName = sheetName;
        this.requiredColumns = requiredColumns;
    }

    @Override
    public final String sheetName() {
        return sheetName;
    }

    @Override
    public final int importSheet(SchoolSetupImportContext context) {
        Optional<ParsedSheet> sheet = context.workbook().sheet(spec());
        if (sheet.isEmpty()) {
            return 0;
        }
        SheetColumns.require(sheet.get(), spec(), requiredColumns);
        importRows(context, sheet.get().rows());
        return sheet.get().rows().size();
    }

    protected abstract void importRows(SchoolSetupImportContext context, List<SpreadsheetRow> rows);

    protected final SheetSpec spec() {
        return SchoolSetupSheets.spec(sheetName);
    }

    protected final RowReader reader(SchoolSetupImportContext context, SpreadsheetRow row) {
        return RowReader.of(row, spec(), context.errors());
    }

    protected final void markFailed(SchoolSetupImportContext context, RowReader row) {
        context.errors().markFailed(sheetName, row.rowNumber());
    }

    /** Error de la fila completa (se referencia la hoja) y la fila cuenta como fallida. */
    protected final void fail(SchoolSetupImportContext context, RowReader row, String message) {
        row.sheetError(message);
        markFailed(context, row);
    }

    protected static String label(String sheetName) {
        return SchoolSetupSheets.spec(sheetName).label();
    }

    protected static List<String> withClassKey(String... columns) {
        List<String> all = new ArrayList<>(SchoolSetupSheets.CLASS_KEY_COLUMNS);
        all.addAll(List.of(columns));
        return all;
    }
}
