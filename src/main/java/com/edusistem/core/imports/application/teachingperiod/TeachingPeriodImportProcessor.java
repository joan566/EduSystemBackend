package com.edusistem.core.imports.application.teachingperiod;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.imports.application.contracts.ImportOutcome;
import com.edusistem.core.imports.application.contracts.ImportProcessor;
import com.edusistem.core.imports.application.contracts.StudentRoster;
import com.edusistem.core.imports.application.contracts.TeachingPeriodSheetImporter;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.ImportProgress;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportType;
import com.edusistem.core.imports.domain.outputports.SpreadsheetReaderPort;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.PeriodWorkbookColumns;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import java.util.List;

/**
 * Importación combinada de un teaching period: hojas Estudiantes, Notas y Asistencia, todas opcionales (también se
 * aceptan los nombres y encabezados en inglés de los archivos antiguos, ver {@link SpreadsheetVocabulary}). Las columnas
 * dinámicas de Notas/Asistencia se reconocen por el id incrustado en su encabezado (ver {@link PeriodWorkbookColumns}),
 * generado por {@code GET /exports/teaching-periods/{id}/full}, que también sirve de plantilla. Cada hoja la importa su
 * propio {@link TeachingPeriodSheetImporter}; una fila o celda incorrecta no aborta el resto de la importación.
 */
public class TeachingPeriodImportProcessor implements ImportProcessor {

    private final SpreadsheetReaderPort reader;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final StudentRoster roster;
    private final List<TeachingPeriodSheetImporter> importers;

    /** @param importers en orden de procesamiento (Estudiantes primero) */
    public TeachingPeriodImportProcessor(SpreadsheetReaderPort reader, TeachingPeriodRepositoryPort teachingPeriods,
                                         StudentRoster roster, List<TeachingPeriodSheetImporter> importers) {
        this.reader = reader;
        this.teachingPeriods = teachingPeriods;
        this.roster = roster;
        this.importers = List.copyOf(importers);
    }

    @Override
    public ImportType type() {
        return ImportType.TEACHING_PERIOD;
    }

    @Override
    public ImportOutcome process(ImportBatch batch, byte[] content, ImportProgress progress) {
        Long teachingPeriodId = batch.getTeachingPeriodId();
        // la propiedad se comprobó al encolar; aquí solo puede faltar si se borró mientras esperaba
        TeachingPeriodView period = teachingPeriods.findViewById(teachingPeriodId)
                .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND",
                        ImportMessages.TEACHING_PERIOD_DELETED));
        TeachingPeriodImportContext context = new TeachingPeriodImportContext(batch.getUserId(), teachingPeriodId,
                period.groupId(), reader.readAll(content), roster, progress);
        progress.start(importers.stream().mapToInt(i -> i.rowCount(context)).sum());
        int totalRows = 0;
        for (TeachingPeriodSheetImporter importer : importers) {
            totalRows += importer.importSheet(context);
        }
        int failedRows = context.errors().failedRows();
        return new ImportOutcome(totalRows, totalRows - failedRows, failedRows, context.errors().list());
    }
}
