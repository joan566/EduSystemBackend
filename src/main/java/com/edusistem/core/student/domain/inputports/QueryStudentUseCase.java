package com.edusistem.core.student.domain.inputports;

import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.student.domain.vo.StudentDetails;
import com.edusistem.core.student.domain.vo.StudentListItem;

public interface QueryStudentUseCase {

    /** Students with their current course: the filtered group's enrollment, else the latest active one. */
    PageResult<StudentListItem> search(Long teacherId, Long groupId, String search, PageQuery page);

    StudentDetails get(Long teacherId, Long studentId);
}
