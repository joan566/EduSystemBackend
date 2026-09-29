package com.edusistem.core.gradebook.domain.inputports;

import com.edusistem.core.gradebook.application.use_case.dtos.GradebookCommands;
import com.edusistem.core.gradebook.domain.entity.RubricCriterion;
import com.edusistem.core.gradebook.domain.vo.EvaluationGradeDetail;
import java.util.List;

public interface ManageRubricUseCase {

    List<RubricCriterion> get(Long teacherId, Long evaluationId);

    List<RubricCriterion> save(GradebookCommands.SaveRubric command);

    void delete(Long teacherId, Long evaluationId);

    EvaluationGradeDetail score(GradebookCommands.ScoreWithRubric command);
}
