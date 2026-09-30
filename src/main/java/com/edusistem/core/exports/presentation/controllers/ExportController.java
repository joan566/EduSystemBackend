package com.edusistem.core.exports.presentation.controllers;

import com.edusistem.core.exports.domain.inputports.ExportSchoolSetupUseCase;
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
    private final ExportSchoolSetupUseCase schoolSetup;

    public ExportController(ExportUseCase exports, ExportSchoolSetupUseCase schoolSetup) {
        this.exports = exports;
        this.schoolSetup = schoolSetup;
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
            description = "3 hojas: Estudiantes, Notas y Asistencia. También sirve como plantilla para "
                    + "POST /imports/teaching-periods/{teachingPeriodId}: se descarga, se edita y se vuelve a subir.")
    public ResponseEntity<byte[]> full(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long teachingPeriodId) {
        return xlsx(exports.full(user.id(), teachingPeriodId));
    }

    @GetMapping("/school-setup")
    @Operation(summary = "Descarga toda la configuración del profesor en un solo Excel",
            description = "Mismo formato que GET /imports/school-setup/template: Instrucciones, Periodos, Grados, "
                    + "Asignaturas, Grupos, Clases (con escala, nota mínima y % Exámenes/Actividades/Asistencia), "
                    + "Horarios, Estudiantes, Actividades, Notas de actividades y Asistencia, con los datos actuales. "
                    + "Sirve como respaldo y se puede editar y volver a subir en POST /imports/school-setup sin crear "
                    + "duplicados. No incluye estudiantes sin número de identificación.")
    public ResponseEntity<byte[]> schoolSetup(@AuthenticationPrincipal AuthenticatedUser user) {
        return xlsx(schoolSetup.schoolSetup(user.id()));
    }

    private static ResponseEntity<byte[]> xlsx(ExportedFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .contentType(MediaType.parseMediaType(XLSX))
                .body(file.content());
    }
}
