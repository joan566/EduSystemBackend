package com.edusistem.core.academic.domain.outputports;

import com.edusistem.core.academic.domain.entity.Group;
import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.Optional;

public interface GroupRepositoryPort {

    Group save(Group group);

    Optional<Group> findById(Long id);

    Optional<GroupView> findViewById(Long id);

    Optional<Group> findByGradeIdAndNameAndAcademicYear(Long gradeId, String name, int academicYear);

    /** Búsqueda por nombre de grado y de grupo (usado por la importación de estudiantes). */
    Optional<Group> findByGradeNameAndNameAndAcademicYear(String gradeName, String name, int academicYear);

    PageResult<GroupView> search(Long gradeId, Integer academicYear, PageQuery page);

    boolean hasDependents(Long groupId);

    void deleteById(Long id);
}
