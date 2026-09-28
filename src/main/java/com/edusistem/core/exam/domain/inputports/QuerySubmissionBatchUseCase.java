package com.edusistem.core.exam.domain.inputports;

import com.edusistem.core.exam.domain.entity.SubmissionBatch;
import com.edusistem.core.exam.domain.vo.SubmissionBatchDetails;
import java.util.List;

public interface QuerySubmissionBatchUseCase {

    SubmissionBatchDetails get(Long teacherId, Long examId, Long batchId);

    List<SubmissionBatch> list(Long teacherId, Long examId);
}
