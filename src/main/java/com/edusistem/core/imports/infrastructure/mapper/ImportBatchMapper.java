package com.edusistem.core.imports.infrastructure.mapper;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.infrastructure.entity.ImportBatchEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ImportBatchMapper {

    ImportBatch toDomain(ImportBatchEntity entity);

    ImportBatchEntity toEntity(ImportBatch batch);
}
