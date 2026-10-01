package com.edusistem.core.exports.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.exports.application.contracts.SchoolSetupSheetExporter;
import com.edusistem.core.exports.application.contracts.SchoolSetupSnapshotLoader;
import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;
import com.edusistem.core.exports.domain.inputports.ExportSchoolSetupUseCase;
import com.edusistem.core.exports.domain.vo.ExportedFile;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.TabularData;
import java.util.ArrayList;
import java.util.List;

/**
 * Exporta toda la configuración del profesor autenticado en el libro de {@link SchoolSetupSheets}: la hoja de
 * instrucciones y luego una hoja por cada {@link SchoolSetupSheetExporter}. Todo se consulta por {@code teacherId}:
 * nunca incluye datos de otro profesor.
 */
public class SchoolSetupExportService implements ExportSchoolSetupUseCase {

    private final SchoolSetupSnapshotLoader loader;
    private final List<SchoolSetupSheetExporter> exporters;
    private final SpreadsheetWriterPort writer;
    private final RecordAuditUseCase audit;

    public SchoolSetupExportService(SchoolSetupSnapshotLoader loader, List<SchoolSetupSheetExporter> exporters,
                                    SpreadsheetWriterPort writer, RecordAuditUseCase audit) {
        this.loader = loader;
        this.exporters = SchoolSetupSheets.inSheetOrder(exporters, SchoolSetupSheetExporter::sheetName);
        this.writer = writer;
        this.audit = audit;
    }

    @Override
    public ExportedFile schoolSetup(Long teacherId) {
        SchoolSetupExportContext context = loader.load(teacherId);
        List<TabularData> sheets = new ArrayList<>();
        sheets.add(SchoolSetupSheets.instructions());
        exporters.forEach(exporter -> sheets.add(exporter.export(context)));
        byte[] content = writer.writeWorkbook(sheets);
        audit.success(teacherId, AuditAction.EXPORT, "SchoolSetup", null,
                context.classes().size() + " classes, " + context.enrollments() + " enrollments");
        return new ExportedFile("configuracion-escolar.xlsx", content);
    }
}
