package com.edusistem.core.student.presentation.controllers;

import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.shared.presentation.PageResponse;
import com.edusistem.core.student.application.use_case.dtos.StudentCommands;
import com.edusistem.core.student.domain.inputports.EraseStudentUseCase;
import com.edusistem.core.student.domain.inputports.QueryStudentUseCase;
import com.edusistem.core.student.domain.inputports.WithdrawStudentUseCase;
import com.edusistem.core.student.presentation.dtos.StudentDtos.StudentDetailResponse;
import com.edusistem.core.student.presentation.dtos.StudentDtos.StudentListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Los estudiantes se dan de alta por importación de Excel; aquí se consultan, se retiran de un grupo o se eliminan. */
@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students")
public class StudentController {

    private final QueryStudentUseCase queryStudents;
    private final WithdrawStudentUseCase withdrawStudent;
    private final EraseStudentUseCase eraseStudent;

    public StudentController(QueryStudentUseCase queryStudents, WithdrawStudentUseCase withdrawStudent,
                             EraseStudentUseCase eraseStudent) {
        this.queryStudents = queryStudents;
        this.withdrawStudent = withdrawStudent;
        this.eraseStudent = eraseStudent;
    }

    @GetMapping
    @Operation(summary = "Lista paginada de los estudiantes del profesor (filtros: groupId, search)")
    public PageResponse<StudentListResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                              @RequestParam(required = false) Long groupId,
                                              @RequestParam(required = false) String search,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size) {
        return PageResponse.from(queryStudents.search(user.id(), groupId, search, PageQuery.of(page, size)),
                StudentListResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un estudiante propio con sus matrículas")
    public StudentDetailResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return StudentDetailResponse.from(queryStudents.get(user.id(), id));
    }

    @PostMapping("/{studentId}/groups/{groupId}/withdrawal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retira a un estudiante de un grupo (conserva el historial de la matrícula)")
    public void withdraw(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long studentId,
                         @PathVariable Long groupId) {
        withdrawStudent.withdraw(new StudentCommands.Withdraw(user.id(), studentId, groupId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina definitivamente al estudiante y todos sus datos (notas, asistencia, hojas, adjuntos)",
            description = "Para solicitudes de supresión de datos personales. No se puede deshacer; para dejar de "
                    + "verlo en un grupo basta con retirarlo.")
    public void erase(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        eraseStudent.erase(user.id(), id);
    }
}
