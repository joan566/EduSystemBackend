package com.edusistem.core.exam.domain.inputports;

import com.edusistem.core.exam.application.use_case.dtos.SubmissionCommands;
import com.edusistem.core.exam.domain.vo.SubmissionDetails;

public interface ReviewSubmissionUseCase {

    SubmissionDetails updateAnswer(SubmissionCommands.UpdateAnswer command);

    SubmissionDetails updateFinalGrade(SubmissionCommands.UpdateFinalGrade command);
}
