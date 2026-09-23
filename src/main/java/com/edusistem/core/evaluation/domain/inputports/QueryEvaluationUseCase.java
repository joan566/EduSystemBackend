package com.edusistem.core.evaluation.domain.inputports;

import com.edusistem.core.evaluation.application.use_case.dtos.EvaluationCommands;
import com.edusistem.core.evaluation.domain.entity.EvaluationCategory;
import com.edusistem.core.evaluation.domain.vo.EvaluationView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.List;

public interface QueryEvaluationUseCase {

    PageResult<EvaluationView> search(Long teacherId, Long teachingPeriodId, Long categoryId, PageQuery page);

    EvaluationView get(Long teacherId, Long evaluationId);

    EvaluationView updateDetails(EvaluationCommands.UpdateDetails command);

    List<EvaluationCategory> listCategories();
}
