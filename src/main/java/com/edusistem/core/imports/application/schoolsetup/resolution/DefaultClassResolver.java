package com.edusistem.core.imports.application.schoolsetup.resolution;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.academic.domain.entity.Group;
import com.edusistem.core.academic.domain.entity.TeachingAssignment;
import com.edusistem.core.academic.domain.entity.TeachingPeriod;
import com.edusistem.core.academic.domain.inputports.ManageTeachingAssignmentUseCase;
import com.edusistem.core.academic.domain.inputports.ManageTeachingPeriodUseCase;
import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingAssignmentRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.imports.application.contracts.ClassResolver;
import com.edusistem.core.imports.application.schoolsetup.ClassKey;
import com.edusistem.core.subject.domain.entity.Subject;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import java.util.Optional;

public class DefaultClassResolver implements ClassResolver {

    private final GroupRepositoryPort groups;
    private final SubjectRepositoryPort subjects;
    private final AcademicPeriodRepositoryPort academicPeriods;
    private final TeachingAssignmentRepositoryPort teachingAssignments;
    private final ManageTeachingAssignmentUseCase manageTeachingAssignment;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final ManageTeachingPeriodUseCase manageTeachingPeriod;

    public DefaultClassResolver(GroupRepositoryPort groups, SubjectRepositoryPort subjects,
                                AcademicPeriodRepositoryPort academicPeriods,
                                TeachingAssignmentRepositoryPort teachingAssignments,
                                ManageTeachingAssignmentUseCase manageTeachingAssignment,
                                TeachingPeriodRepositoryPort teachingPeriods,
                                ManageTeachingPeriodUseCase manageTeachingPeriod) {
        this.groups = groups;
        this.subjects = subjects;
        this.academicPeriods = academicPeriods;
        this.teachingAssignments = teachingAssignments;
        this.manageTeachingAssignment = manageTeachingAssignment;
        this.teachingPeriods = teachingPeriods;
        this.manageTeachingPeriod = manageTeachingPeriod;
    }

    @Override
    public Optional<Long> find(Long teacherId, ClassKey key) {
        Optional<Group> group = groups.findByGradeNameAndNameAndAcademicYear(teacherId, key.gradeName(), key.groupName(),
                key.academicYear());
        Optional<Subject> subject = subjects.findByTeacherIdAndName(teacherId, key.subjectName());
        Optional<AcademicPeriod> period = academicPeriods.findByTeacherIdAndName(teacherId, key.academicPeriodName());
        if (group.isEmpty() || subject.isEmpty() || period.isEmpty()) {
            return Optional.empty();
        }
        return teachingAssignments
                .findByTeacherIdAndGroupIdAndSubjectId(teacherId, group.get().getId(), subject.get().getId())
                .flatMap(a -> teachingPeriods.findByTeachingAssignmentIdAndAcademicPeriodId(a.getId(), period.get().getId()))
                .map(TeachingPeriod::getId);
    }

    @Override
    public Long findOrCreate(Long teacherId, Long groupId, Long subjectId, Long academicPeriodId) {
        Long assignmentId = teachingAssignments.findByTeacherIdAndGroupIdAndSubjectId(teacherId, groupId, subjectId)
                .map(TeachingAssignment::getId)
                .orElseGet(() -> manageTeachingAssignment
                        .create(new AcademicCommands.CreateTeachingAssignment(teacherId, groupId, subjectId)).id());
        return teachingPeriods.findByTeachingAssignmentIdAndAcademicPeriodId(assignmentId, academicPeriodId)
                .map(TeachingPeriod::getId)
                .orElseGet(() -> manageTeachingPeriod
                        .create(new AcademicCommands.CreateTeachingPeriod(teacherId, assignmentId, academicPeriodId)).id());
    }
}
