package com.edusistem.core.activity.domain.inputports;

import com.edusistem.core.activity.application.use_case.dtos.ActivityCommands;
import com.edusistem.core.activity.domain.vo.StudentGradeView;
import java.util.List;

public interface GradeActivityUseCase {

    /** Registra o corrige notas (todas o ninguna). */
    List<StudentGradeView> recordGrades(ActivityCommands.RecordGrades command);

    /** Un elemento por estudiante activo del grupo, con su nota si existe. */
    List<StudentGradeView> listGrades(Long teacherId, Long activityId);
}
