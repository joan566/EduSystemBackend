package com.edusistem.core.exam.domain.vo;

import com.edusistem.core.exam.domain.entity.SubmissionBatch;
import com.edusistem.core.exam.domain.entity.SubmissionBatchPage;
import java.util.List;
import java.util.Map;

/** Estado del lote con sus páginas y el estado ACTUAL de cada submission (refleja revisiones manuales posteriores). */
public record SubmissionBatchDetails(SubmissionBatch batch, List<SubmissionBatchPage> pages,
                                     Map<Long, ExamSubmissionSummary> submissionsById) {
}
