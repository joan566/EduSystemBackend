package com.edusistem.core.imports.application.use_case.service;

import static com.edusistem.core.imports.application.student.StudentImportSheet.SHEET;
import static com.edusistem.core.imports.application.student.StudentImportSheet.TEMPLATE_COLUMNS;

import com.edusistem.core.imports.application.contracts.ImportQueue;
import com.edusistem.core.imports.application.contracts.ImportQueue.ImportRequest;
import com.edusistem.core.imports.application.student.StudentsImportProcessor;
import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportType;
import com.edusistem.core.imports.domain.inputports.ImportStudentsUseCase;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import java.util.List;

/** Encola la importación de estudiantes (la aplica {@link StudentsImportProcessor}) y genera su plantilla. */
public class ImportStudentsService implements ImportStudentsUseCase {

    private final ImportQueue queue;
    private final SpreadsheetWriterPort writer;

    public ImportStudentsService(ImportQueue queue, SpreadsheetWriterPort writer) {
        this.queue = queue;
        this.writer = writer;
    }

    @Override
    public ImportBatch importStudents(ImportCommands.ImportStudents command) {
        return queue.submit(new ImportRequest(command.teacherId(), ImportType.STUDENTS, null, command.fileName(),
                "students.xlsx", command.content()));
    }

    @Override
    public byte[] template() {
        return writer.write(SHEET.table(TEMPLATE_COLUMNS, List.of(
                List.<Object>of("1001234567", "Ana", "Pérez", "ana.perez@example.com", "10°", "A", 2026))));
    }
}
