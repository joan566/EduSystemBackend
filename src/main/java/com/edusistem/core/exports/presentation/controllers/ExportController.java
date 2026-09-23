package com.edusistem.core.exports.presentation.controllers;

import com.edusistem.core.exports.domain.inputports.ExportUseCase;
import com.edusistem.core.exports.domain.vo.ExportedFile;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exports")
@Tag(name = "Exports")
public class ExportController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExportUseCase exports;

    public ExportController(ExportUseCase exports) {
        this.exports = exports;
    }

    @GetMapping("/students")
    @Operation(summary = "Exporta estudiantes a Excel (filtros opcionales: teachingPeriodId o groupId)")
    public ResponseEntity<byte[]> students(@AuthenticationPrincipal AuthenticatedUser user,
                                           @RequestParam(required = false) Long teachingPeriodId,
                                           @RequestParam(required = false) Long groupId) {
        return xlsx(exports.students(user.id(), groupId, teachingPeriodId));
    }

    @GetMapping("/grades")
    @Operation(summary = "Exporta las calificaciones de un teaching period a Excel")
    public ResponseEntity<byte[]> grades(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam Long teachingPeriodId) {
        return xlsx(exports.grades(user.id(), teachingPeriodId));
    }

    @GetMapping("/attendance")
    @Operation(summary = "Exporta la asistencia de un teaching period a Excel")
    public ResponseEntity<byte[]> attendance(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam Long teachingPeriodId) {
        return xlsx(exports.attendance(user.id(), teachingPeriodId));
    }

    @GetMapping("/teaching-periods/{teachingPeriodId}/full")
    @Operation(summary = "Exporta estudiantes, notas de actividades y asistencia de un teaching period en un solo Excel",
            description = "3 hojas: Students, Grades y Attendance. También sirve como plantilla para "
                    + "POST /imports/teaching-periods/{teachingPeriodId}: se descarga, se edita y se vuelve a subir.")
    public ResponseEntity<byte[]> full(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long teachingPeriodId) {
        return xlsx(exports.full(user.id(), teachingPeriodId));
    }

    private static ResponseEntity<byte[]> xlsx(ExportedFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .contentType(MediaType.parseMediaType(XLSX))
                .body(file.content());
    }
}
