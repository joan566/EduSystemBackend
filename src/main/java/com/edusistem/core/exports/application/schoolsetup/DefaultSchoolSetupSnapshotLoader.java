package com.edusistem.core.exports.application.schoolsetup;

import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.activity.domain.outputports.ActivityRepositoryPort;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.exports.application.contracts.SchoolSetupSnapshotLoader;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Todo se consulta por {@code teacherId}. Los estudiantes sin número de identificación no se incluyen (el importador
 * los identifica por ese número).
 */
public class DefaultSchoolSetupSnapshotLoader implements SchoolSetupSnapshotLoader {

    private final GroupRepositoryPort groups;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final StudentRepositoryPort students;
    private final GradingScaleRepositoryPort scales;
    private final ActivityRepositoryPort activities;

    public DefaultSchoolSetupSnapshotLoader(GroupRepositoryPort groups, TeachingPeriodRepositoryPort teachingPeriods,
                                            StudentRepositoryPort students, GradingScaleRepositoryPort scales,
                                            ActivityRepositoryPort activities) {
        this.groups = groups;
        this.teachingPeriods = teachingPeriods;
        this.students = students;
        this.scales = scales;
        this.activities = activities;
    }

    @Override
    public SchoolSetupExportContext load(Long teacherId) {
        List<GroupView> groupList = PageResult.collectAll(page -> groups.search(teacherId, null, null, page));
        List<TeachingPeriodView> classes = PageResult.collectAll(
                page -> teachingPeriods.findViewsByTeacherId(teacherId, null, null, page));
        classes.sort(Comparator.comparing(TeachingPeriodView::academicYear).thenComparing(TeachingPeriodView::gradeName)
                .thenComparing(TeachingPeriodView::groupName).thenComparing(TeachingPeriodView::subjectName)
                .thenComparing(TeachingPeriodView::startDate));

        Map<Long, List<Student>> rosterByGroup = new HashMap<>();
        for (GroupView group : groupList) {
            rosterByGroup.put(group.id(), students.findActiveByGroupId(group.id()).stream()
                    .filter(s -> s.getIdentificationNumber() != null).toList());
        }
        Map<Long, List<ActivityView>> activitiesByClass = new HashMap<>();
        for (TeachingPeriodView tp : classes) {
            activitiesByClass.put(tp.id(),
                    PageResult.collectAll(page -> activities.findViewsByTeachingPeriodId(tp.id(), page)));
        }
        return new SchoolSetupExportContext(teacherId, groupList, classes, rosterByGroup,
                scales.findVisibleTo(teacherId), activitiesByClass);
    }
}
