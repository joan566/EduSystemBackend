package com.edusistem.core.gradebook.domain.inputports;

import com.edusistem.core.gradebook.application.use_case.dtos.GradebookCommands;
import com.edusistem.core.gradebook.domain.entity.GradeAttachment;
import com.edusistem.core.gradebook.domain.vo.StoredFile;

public interface ManageGradeAttachmentUseCase {

    GradeAttachment upload(GradebookCommands.UploadAttachment command);

    StoredFile download(Long teacherId, Long evaluationId, Long studentId);

    void delete(Long teacherId, Long evaluationId, Long studentId);
}
