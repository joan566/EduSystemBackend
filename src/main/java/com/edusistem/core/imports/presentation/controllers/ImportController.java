package com.edusistem.core.imports.presentation.controllers;

import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.inputports.ImportSchoolSetupUseCase;
import com.edusistem.core.imports.domain.inputports.ImportStudentsUseCase;
import com.edusistem.core.imports.domain.inputports.ImportTeachingPeriodDataUseCase;
import com.edusistem.core.imports.domain.inputports.QueryImportUseCase;
import com.edusistem.core.imports.presentation.dtos.ImportDtos.ImportBatchResponse;
import com.edusistem.core.imports.presentation.dtos.ImportDtos.ImportResultResponse;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.shared.presentation.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports")
@Tag(name = "Imports")
public class ImportController {

    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ImportStudentsUseCase importStudents;
    private final ImportTeachingPeriodDataUseCase importTeachingPeriodData;
    private final ImportSchoolSetupUseCase importSchoolSetup;
    private final QueryImportUseCase queryImports;

    public ImportController(ImportStudentsUseCase importStudents, ImportTeachingPeriodDataUseCase importTeachingPeriodData,
                            ImportSchoolSetupUseCase importSchoolSetup, QueryImportUseCase queryImports) {
        this.importStudents = importStudents;
        this.importTeachingPeriodData = importTeachingPeriodData;
        this.importSchoolSetup = importSchoolSetup;
        this.queryImports = queryImports;
    }

    @PostMapping(value = "/students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Importa estudiantes desde un Excel (.xlsx)",
            description = "Columnas: identification_number, first_name, last_name, email, grade, group "
                    + "(opcionales: academic_year, student_code). Las filas inválidas no detienen la importación.")
    public ImportResultResponse importStudents(@AuthenticationPrincipal AuthenticatedUser user,
                                               @RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidRequestException("EMPTY_FILE", "The uploaded file is empty");
        }
        return ImportResultResponse.from(importStudents.importStudents(
                new ImportCommands.ImportStudents(user.id(), file.getOriginalFilename(), file.getBytes())));
    }

    @GetMapping("/students/template")
    @Operation(summary = "Descarga la plantilla Excel para importar estudiantes")
    public ResponseEntity<byte[]> template() {
        return download(importStudents.template(), "students-template.xlsx");
    }

    @PostMapping(value = "/teaching-periods/{teachingPeriodId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Importa estudiantes, notas de actividades y asistencia de un teaching period desde un Excel combinado",
            description = "Hojas opcionales: Students (identification_number, first_name, last_name, email), Grades "
                    + "(identification_number + una columna por actividad) y Attendance (identification_number + una "
                    + "columna por sesión). GET /exports/teaching-periods/{teachingPeriodId}/full genera el archivo "
                    + "(con los datos actuales) listo para editar y volver a subir.")
    public ImportResultResponse importTeachingPeriodData(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @PathVariable Long teachingPeriodId,
                                                         @RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidRequestException("EMPTY_FILE", "The uploaded file is empty");
        }
        return ImportResultResponse.from(importTeachingPeriodData.importData(new ImportCommands.ImportTeachingPeriodData(
                user.id(), teachingPeriodId, file.getOriginalFilename(), file.getBytes())));
    }

    @GetMapping("/school-setup/template")
    @Operation(summary = "Descarga la plantilla Excel completa (9 hojas) para importar toda la configuración de un profesor",
            description = "Hojas opcionales: AcademicPeriods, AcademicGrades, Subjects, Groups, Classes, Students, "
                    + "Activities, ActivityGrades y Attendance. Se procesan en ese orden: cada hoja referencia sus "
                    + "dependencias por nombre (no por id) y se crean solo si aún no existen.")
    public ResponseEntity<byte[]> schoolSetupTemplate() {
        return download(importSchoolSetup.template(), "school-setup-template.xlsx");
    }

    @PostMapping(value = "/school-setup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Importa la configuración completa de un profesor desde un Excel combinado (9 hojas)",
            description = "Crea periodos académicos, grados, materias, cursos, clases, estudiantes, actividades, "
                    + "calificaciones y asistencia en una sola subida. GET /imports/school-setup/template genera el "
                    + "archivo listo para editar y subir.")
    public ImportResultResponse importSchoolSetup(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidRequestException("EMPTY_FILE", "The uploaded file is empty");
        }
        return ImportResultResponse.from(importSchoolSetup.importData(
                new ImportCommands.ImportSchoolSetup(user.id(), file.getOriginalFilename(), file.getBytes())));
    }

    @GetMapping
    @Operation(summary = "Lista paginada de las importaciones del usuario")
    public PageResponse<ImportBatchResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        return PageResponse.from(queryImports.list(user.id(), PageQuery.of(page, size)), ImportBatchResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta una importación propia")
    public ImportBatchResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return ImportBatchResponse.from(queryImports.get(user.id(), id));
    }

    @GetMapping("/{id}/error-report")
    @Operation(summary = "Descarga el reporte de errores (.xlsx) de una importación")
    public ResponseEntity<byte[]> errorReport(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return download(queryImports.errorReport(user.id(), id), "import-" + id + "-errors.xlsx");
    }

    private static ResponseEntity<byte[]> download(byte[] content, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(MediaType.parseMediaType(XLSX))
                .body(content);
    }
}
