package com.edusistem.core.exam.infrastructure.mapper;

import com.edusistem.core.exam.domain.entity.SubmissionBatch;
import com.edusistem.core.exam.domain.entity.SubmissionBatchPage;
import com.edusistem.core.exam.infrastructure.entity.SubmissionBatchEntity;
import com.edusistem.core.exam.infrastructure.entity.SubmissionBatchPageEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubmissionBatchMapper {

    SubmissionBatch toDomain(SubmissionBatchEntity entity);

    SubmissionBatchEntity toEntity(SubmissionBatch batch);

    SubmissionBatchPage toDomain(SubmissionBatchPageEntity entity);

    SubmissionBatchPageEntity toEntity(SubmissionBatchPage page);
}
