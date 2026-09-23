package com.edusistem.core.imports.domain.entity;

import com.edusistem.core.imports.domain.enums.ImportStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportBatch {
    private Long id;
    private Long userId;
    private String fileName;
    private String filePath;
    private ImportStatus status;
    private Integer totalRows;
    private Integer successfulRows;
    private Integer failedRows;
    private String errorReportPath;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
