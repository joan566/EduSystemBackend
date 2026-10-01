package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.imports.application.contracts.ClassGradingConfigurer;
import com.edusistem.core.imports.application.contracts.ImportBatchTracker;
import com.edusistem.core.imports.application.contracts.ImportBatchTracker.ImportOutcome;
import com.edusistem.core.imports.application.contracts.SchoolSetupSheetImporter;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.inputports.ImportSchoolSetupUseCase;
import com.edusistem.core.imports.domain.outputports.SpreadsheetReaderPort;
import com.edusistem.core.imports.domain.vo.ImportResult;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import com.edusistem.core.shared.domain.vo.TabularData;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Importación combinada de la configuración completa de un profesor: 10 hojas, todas opcionales, procesadas en el orden
 * de dependencia de {@link SchoolSetupSheets} (cada hoja la importa su propio {@link SchoolSetupSheetImporter}). A
 * diferencia de {@link ImportTeachingPeriodDataService} (que edita un teaching period que ya existe, con columnas
 * dinámicas por id), aquí los ids todavía no existen: cada hoja referencia sus dependencias por nombre y se resuelven
 * ("buscar o crear") a medida que se procesan las hojas. Una fila o celda incorrecta no aborta el resto.
 */
public class ImportSchoolSetupService implements ImportSchoolSetupUseCase {

    private final ImportBatchTracker batches;
    private final SpreadsheetReaderPort reader;
    private final SpreadsheetWriterPort writer;
    private final ClassGradingConfigurer grading;
    private final List<SchoolSetupSheetImporter> importers;

    public ImportSchoolSetupService(ImportBatchTracker batches, SpreadsheetReaderPort reader,
                                    SpreadsheetWriterPort writer, ClassGradingConfigurer grading,
                                    List<SchoolSetupSheetImporter> importers) {
        this.batches = batches;
        this.reader = reader;
        this.writer = writer;
        this.grading = grading;
        this.importers = SchoolSetupSheets.inSheetOrder(importers, SchoolSetupSheetImporter::sheetName);
    }

    @Override
    public ImportResult importData(ImportCommands.ImportSchoolSetup command) {
        Long teacherId = command.teacherId();
        return batches.track(teacherId, command.fileName(), "school-setup.xlsx", command.content(), () -> {
            SchoolSetupImportContext context = new SchoolSetupImportContext(teacherId, reader.readAll(command.content()));
            int totalRows = 0;
            for (SchoolSetupSheetImporter importer : importers) {
                totalRows += importer.importSheet(context);
            }
            int failedRows = context.errors().failedRows();
            return new ImportOutcome(totalRows, totalRows - failedRows, failedRows, context.errors().list());
        });
    }

    @Override
    public byte[] template() {
        List<Object> exampleClass = List.of("10°", "A", 2026, "Matemáticas", "2026-1");
        return writer.writeWorkbook(List.of(
                SchoolSetupSheets.instructions(),
                table(SchoolSetupSheets.ACADEMIC_PERIODS,
                        List.of("2026-1", LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 15))),
                table(SchoolSetupSheets.ACADEMIC_GRADES, List.of("10°", "Décimo grado")),
                table(SchoolSetupSheets.SUBJECTS, List.of("Matemáticas", "Matemáticas de grado 10")),
                table(SchoolSetupSheets.GROUPS, List.of("10°", "A", 2026)),
                SchoolSetupSheets.table(SchoolSetupSheets.CLASSES, List.of(withClass(exampleClass, "0-5", 3, 40, 40, 20)),
                        grading.scaleOptions(null)),
                table(SchoolSetupSheets.SCHEDULES, withClass(exampleClass,
                        SpreadsheetVocabulary.dayLabel(DayOfWeek.MONDAY), LocalTime.of(7, 0), LocalTime.of(8, 0),
                        "Salón 201")),
                table(SchoolSetupSheets.STUDENTS,
                        List.of("1001234567", "Ana", "Pérez", "ana.perez@example.com", "10°", "A", 2026)),
                table(SchoolSetupSheets.ACTIVITIES, withClass(exampleClass, "Taller 1", "Taller de repaso",
                        LocalDateTime.of(2026, 2, 10, 9, 0), BigDecimal.valueOf(5), "Taller")),
                table(SchoolSetupSheets.ACTIVITY_GRADES,
                        withClass(exampleClass, "Taller 1", "1001234567", BigDecimal.valueOf(4.5))),
                table(SchoolSetupSheets.ATTENDANCE, withClass(exampleClass, LocalDate.of(2026, 2, 10), "1001234567",
                        SpreadsheetVocabulary.attendanceLabel("PRESENT")))));
    }

    private static TabularData table(String sheetName, List<Object> exampleRow) {
        return SchoolSetupSheets.table(sheetName, List.of(exampleRow), List.of());
    }

    private static List<Object> withClass(List<Object> classKey, Object... values) {
        List<Object> row = new ArrayList<>(classKey);
        row.addAll(List.of(values));
        return row;
    }
}
