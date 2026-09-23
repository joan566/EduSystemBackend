package com.edusistem.core.academic.domain.inputports;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.entity.Grade;
import java.util.List;

public interface ManageGradeUseCase {

    Grade create(AcademicCommands.CreateGrade command);

    Grade update(AcademicCommands.UpdateGrade command);

    void delete(Long actorId, Long gradeId);

    Grade get(Long gradeId);

    List<Grade> list();
}
