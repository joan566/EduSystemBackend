package com.edusistem.core.imports.application.schoolsetup;

import com.edusistem.core.imports.application.contracts.ImportOutcome;
import com.edusistem.core.imports.application.contracts.ImportProcessor;
import com.edusistem.core.imports.application.contracts.SchoolSetupSheetImporter;
import com.edusistem.core.imports.application.support.ImportProgress;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportType;
import com.edusistem.core.imports.domain.outputports.SpreadsheetReaderPort;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import java.util.List;

/**
 * Importación combinada de la configuración completa de un profesor: 10 hojas, todas opcionales, procesadas en el orden
 * de dependencia de {@link SchoolSetupSheets} (cada hoja la importa su propio {@link SchoolSetupSheetImporter}). A
 * diferencia de la importación de un teaching period (que edita uno que ya existe, con columnas dinámicas por id), aquí
 * los ids todavía no existen: cada hoja referencia sus dependencias por nombre y se resuelven ("buscar o crear") a
 * medida que se procesan las hojas. Una fila o celda incorrecta no aborta el resto.
 */
public class SchoolSetupImportProcessor implements ImportProcessor {

    private final SpreadsheetReaderPort reader;
    private final List<SchoolSetupSheetImporter> importers;

    public SchoolSetupImportProcessor(SpreadsheetReaderPort reader, List<SchoolSetupSheetImporter> importers) {
        this.reader = reader;
        this.importers = SchoolSetupSheets.inSheetOrder(importers, SchoolSetupSheetImporter::sheetName);
    }

    @Override
    public ImportType type() {
        return ImportType.SCHOOL_SETUP;
    }

    @Override
    public ImportOutcome process(ImportBatch batch, byte[] content, ImportProgress progress) {
        SchoolSetupImportContext context = new SchoolSetupImportContext(batch.getUserId(), reader.readAll(content),
                progress);
        progress.start(importers.stream().mapToInt(i -> i.rowCount(context)).sum());
        int totalRows = 0;
        for (SchoolSetupSheetImporter importer : importers) {
            totalRows += importer.importSheet(context);
        }
        int failedRows = context.errors().failedRows();
        return new ImportOutcome(totalRows, totalRows - failedRows, failedRows, context.errors().list());
    }
}
