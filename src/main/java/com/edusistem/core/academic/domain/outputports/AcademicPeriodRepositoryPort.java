package com.edusistem.core.academic.domain.outputports;

import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.Optional;

public interface AcademicPeriodRepositoryPort {

    AcademicPeriod save(AcademicPeriod period);

    Optional<AcademicPeriod> findById(Long id);

    Optional<AcademicPeriod> findByName(String name);

    PageResult<AcademicPeriod> findAll(PageQuery page);

    boolean hasTeachingPeriods(Long academicPeriodId);

    void deleteById(Long id);
}
