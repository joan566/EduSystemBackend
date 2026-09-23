package com.edusistem.core.exam.domain.inputports;

import com.edusistem.core.exam.application.use_case.dtos.ExamCommands;
import com.edusistem.core.exam.domain.vo.ExamDetails;
import com.edusistem.core.exam.domain.vo.ExamView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public interface ManageExamUseCase {

    ExamDetails create(ExamCommands.Create command);

    ExamDetails get(Long teacherId, Long examId);

    ExamDetails update(ExamCommands.Update command);

    /** Reemplaza todas las preguntas; no se permite si ya existen submissions. */
    ExamDetails replaceQuestions(ExamCommands.ReplaceQuestions command);

    void delete(Long teacherId, Long examId);

    PageResult<ExamView> search(Long teacherId, Long teachingPeriodId, PageQuery page);
}
