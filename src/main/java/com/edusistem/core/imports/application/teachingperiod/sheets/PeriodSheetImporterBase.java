package com.edusistem.core.imports.application.teachingperiod.sheets;

import com.edusistem.core.imports.application.contracts.TeachingPeriodSheetImporter;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.application.support.SheetColumns;
import com.edusistem.core.imports.application.teachingperiod.TeachingPeriodImportContext;
import com.edusistem.core.imports.domain.vo.ParsedSheet;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.PeriodWorkbookColumns;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary.SheetSpec;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Pasos comunes: la hoja es opcional y sus columnas obligatorias deben estar. */
abstract class PeriodSheetImporterBase implements TeachingPeriodSheetImporter {

    private final SheetSpec spec;
    private final List<String> requiredColumns;

    protected PeriodSheetImporterBase(SheetSpec spec, List<String> requiredColumns) {
        this.spec = spec;
        this.requiredColumns = requiredColumns;
    }

    @Override
    public final int importSheet(TeachingPeriodImportContext context) {
        Optional<ParsedSheet> sheet = context.workbook().sheet(spec);
        if (sheet.isEmpty()) {
            return 0;
        }
        SheetColumns.require(sheet.get(), spec, requiredColumns);
        context.progress().step(spec.label());
        importRows(context, sheet.get());
        return sheet.get().rows().size();
    }

    @Override
    public final int rowCount(TeachingPeriodImportContext context) {
        return context.workbook().sheet(spec).map(s -> s.rows().size()).orElse(0);
    }

    protected abstract void importRows(TeachingPeriodImportContext context, ParsedSheet sheet);

    protected final SheetSpec spec() {
        return spec;
    }

    /** Se pide una vez por fila: también cuenta la fila en el avance. */
    protected final RowReader reader(TeachingPeriodImportContext context, SpreadsheetRow row) {
        context.progress().tick();
        return RowReader.of(row, spec, context.errors());
    }

    protected final void markFailed(TeachingPeriodImportContext context, RowReader row) {
        context.errors().markFailed(spec.name(), row.rowNumber());
    }

    /**
     * Estudiante de la fila en las hojas de columnas dinámicas: obligatorio, una sola fila por estudiante y activo en el
     * grupo. Si no, registra el error, marca la fila como fallida y devuelve null.
     */
    protected final Long rosterStudent(TeachingPeriodImportContext context, RowReader row, Set<String> seen) {
        String identification = row.raw("identification_number");
        String problem = identification == null ? ImportMessages.REQUIRED
                : !seen.add(identification) ? ImportMessages.DUPLICATED_IN_SHEET
                : !context.roster().containsKey(identification) ? ImportMessages.STUDENT_NOT_IN_GROUP
                : null;
        if (problem != null) {
            row.error("identification_number", problem);
            markFailed(context, row);
            return null;
        }
        return context.roster().get(identification);
    }

    /**
     * Columnas dinámicas de la hoja (encabezado normalizado → actividad/sesión) según el id incrustado. Solo se tienen
     * en cuenta los ids que pertenecen al teaching period; si dos columnas traen el mismo id, vale la primera.
     */
    protected static <T> Map<String, T> columnsById(ParsedSheet sheet, Map<Long, T> byId) {
        Map<String, T> byHeader = new LinkedHashMap<>();
        Set<Long> used = new HashSet<>();
        for (String header : sheet.headers()) {
            PeriodWorkbookColumns.idFromHeader(header)
                    .filter(byId::containsKey)
                    .filter(used::add)
                    .ifPresent(id -> byHeader.put(header, byId.get(id)));
        }
        return byHeader;
    }
}
