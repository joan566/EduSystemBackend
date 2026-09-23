package com.edusistem.core.subject.domain.inputports;

import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.subject.application.use_case.dtos.SubjectCommands;
import com.edusistem.core.subject.domain.entity.Subject;

public interface ManageSubjectUseCase {

    Subject create(SubjectCommands.Create command);

    Subject update(SubjectCommands.Update command);

    void delete(Long actorId, Long subjectId);

    Subject get(Long subjectId);

    PageResult<Subject> search(String nameQuery, PageQuery page);
}
