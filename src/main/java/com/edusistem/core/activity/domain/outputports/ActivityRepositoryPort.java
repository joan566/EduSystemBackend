package com.edusistem.core.activity.domain.outputports;

import com.edusistem.core.activity.domain.entity.Activity;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.Optional;

public interface ActivityRepositoryPort {

    Activity save(Activity activity);

    Optional<Activity> findById(Long id);

    Optional<ActivityView> findViewById(Long id);

    PageResult<ActivityView> findViewsByTeachingPeriodId(Long teachingPeriodId, PageQuery page);

    void deleteById(Long id);
}
