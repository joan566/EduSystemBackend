package com.edusistem.core.student.domain.inputports;

import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.vo.StudentDetails;

public interface QueryStudentUseCase {

    PageResult<Student> search(Long teacherId, Long groupId, String search, PageQuery page);

    StudentDetails get(Long teacherId, Long studentId);
}
