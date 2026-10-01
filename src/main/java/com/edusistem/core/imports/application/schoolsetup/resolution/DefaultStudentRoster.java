package com.edusistem.core.imports.application.schoolsetup.resolution;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.imports.application.contracts.StudentRoster;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultStudentRoster implements StudentRoster {

    private final StudentRepositoryPort students;
    private final TeachingPeriodRepositoryPort teachingPeriods;

    public DefaultStudentRoster(StudentRepositoryPort students, TeachingPeriodRepositoryPort teachingPeriods) {
        this.students = students;
        this.teachingPeriods = teachingPeriods;
    }

    @Override
    public Map<String, Long> activeByIdentificationInGroup(Long groupId) {
        return students.findActiveByGroupId(groupId).stream()
                .filter(s -> s.getIdentificationNumber() != null)
                .collect(Collectors.toMap(Student::getIdentificationNumber, Student::getId, (a, b) -> a));
    }

    @Override
    public Map<String, Long> activeByIdentificationInTeachingPeriod(Long teachingPeriodId) {
        TeachingPeriodView period = teachingPeriods.findViewById(teachingPeriodId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", teachingPeriodId));
        return activeByIdentificationInGroup(period.groupId());
    }
}
