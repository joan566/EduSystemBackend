package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.imports.application.contracts.ImportBatchTracker;
import com.edusistem.core.imports.application.contracts.ImportBatchTracker.ImportOutcome;
import com.edusistem.core.imports.application.contracts.StudentRoster;
import com.edusistem.core.imports.application.contracts.TeachingPeriodSheetImporter;
import com.edusistem.core.imports.application.teachingperiod.TeachingPeriodImportContext;
import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.inputports.ImportTeachingPeriodDataUseCase;
import com.edusistem.core.imports.domain.outputports.SpreadsheetReaderPort;
import com.edusistem.core.imports.domain.vo.ImportResult;
import com.edusistem.core.shared.application.service.OwnershipGuard;
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
public class ImportTeachingPeriodDataService implements ImportTeachingPeriodDataUseCase {

    private final ImportBatchTracker batches;
    private final SpreadsheetReaderPort reader;
    private final OwnershipGuard guard;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final StudentRoster roster;
    private final List<TeachingPeriodSheetImporter> importers;

    /** @param importers en orden de procesamiento (Estudiantes primero) */
    public ImportTeachingPeriodDataService(ImportBatchTracker batches, SpreadsheetReaderPort reader,
                                           OwnershipGuard guard, TeachingPeriodRepositoryPort teachingPeriods,
                                           StudentRoster roster, List<TeachingPeriodSheetImporter> importers) {
        this.batches = batches;
        this.reader = reader;
        this.guard = guard;
        this.teachingPeriods = teachingPeriods;
        this.roster = roster;
        this.importers = List.copyOf(importers);
    }

    @Override
    public ImportResult importData(ImportCommands.ImportTeachingPeriodData command) {
        Long teacherId = command.teacherId();
        Long teachingPeriodId = command.teachingPeriodId();
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        TeachingPeriodView period = teachingPeriods.findViewById(teachingPeriodId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", teachingPeriodId));

        return batches.track(teacherId, command.fileName(), "teaching-period.xlsx", command.content(), () -> {
            TeachingPeriodImportContext context = new TeachingPeriodImportContext(teacherId, teachingPeriodId,
                    period.groupId(), reader.readAll(command.content()), roster);
            int totalRows = 0;
            for (TeachingPeriodSheetImporter importer : importers) {
                totalRows += importer.importSheet(context);
            }
            int failedRows = context.errors().failedRows();
            return new ImportOutcome(totalRows, totalRows - failedRows, failedRows, context.errors().list());
        });
    }
}
