package com.edusistem.core.academic.domain.inputports;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public interface ManageAcademicPeriodUseCase {

    AcademicPeriod create(AcademicCommands.SavePeriod command);

    AcademicPeriod update(AcademicCommands.SavePeriod command);

    void delete(Long teacherId, Long periodId);

    AcademicPeriod get(Long teacherId, Long periodId);

    PageResult<AcademicPeriod> list(Long teacherId, PageQuery page);
}
