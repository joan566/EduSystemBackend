package com.edusistem.core.grading.domain.inputports;

import com.edusistem.core.grading.application.use_case.dtos.GradingCommands;
import com.edusistem.core.grading.domain.entity.GradingScale;
import java.util.List;

public interface ManageGradingScaleUseCase {

    GradingScale create(GradingCommands.CreateScale command);

    GradingScale get(Long scaleId);

    List<GradingScale> list();
}
