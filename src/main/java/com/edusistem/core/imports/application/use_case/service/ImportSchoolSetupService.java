package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.imports.application.contracts.ClassGradingConfigurer;
import com.edusistem.core.imports.application.contracts.ImportQueue;
import com.edusistem.core.imports.application.contracts.ImportQueue.ImportRequest;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportProcessor;
import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportType;
import com.edusistem.core.imports.domain.inputports.ImportSchoolSetupUseCase;
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
 * Encola la importación de la configuración completa de un profesor (la aplica {@link SchoolSetupImportProcessor}) y
 * genera su plantilla.
 */
public class ImportSchoolSetupService implements ImportSchoolSetupUseCase {

    private final ImportQueue queue;
    private final SpreadsheetWriterPort writer;
    private final ClassGradingConfigurer grading;

    public ImportSchoolSetupService(ImportQueue queue, SpreadsheetWriterPort writer, ClassGradingConfigurer grading) {
        this.queue = queue;
        this.writer = writer;
        this.grading = grading;
    }

    @Override
    public ImportBatch importData(ImportCommands.ImportSchoolSetup command) {
        return queue.submit(new ImportRequest(command.teacherId(), ImportType.SCHOOL_SETUP, null, command.fileName(),
                "school-setup.xlsx", command.content()));
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
