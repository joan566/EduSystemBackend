package com.edusistem.core.imports.domain.entity;

import com.edusistem.core.imports.domain.enums.ImportStatus;
import com.edusistem.core.imports.domain.enums.ImportType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un Excel subido que se importa en segundo plano. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportBatch {
    private Long id;
    private Long userId;
    /** Nulo en las importaciones síncronas anteriores a la cola. */
    private ImportType importType;
    /** Solo en las importaciones de un teaching period. */
    private Long teachingPeriodId;
    private String fileName;
    private String filePath;
    private ImportStatus status;
    /** Se fija al leer el archivo, antes de procesarlo; el total definitivo queda al terminar. */
    private Integer totalRows;
    /** Filas ya recorridas mientras corre (ver {@code ImportProgress}). */
    private Integer processedRows;
    /** Hoja que se está procesando; nula al terminar. */
    private String currentStep;
    private Integer successfulRows;
    private Integer failedRows;
    private String errorReportPath;
    /** Por qué falló el archivo entero (no por filas). */
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public boolean isFinished() {
        return status != ImportStatus.QUEUED && status != ImportStatus.PROCESSING;
    }

    public void start(LocalDateTime now) {
        status = ImportStatus.PROCESSING;
        // reanudar tras un reinicio vuelve a recorrer el archivo desde el principio
        processedRows = 0;
        currentStep = null;
        if (startedAt == null) {
            startedAt = now;
        }
    }

    /** Parte de 100 recorrida; 100 si ya terminó (también si falló). */
    public int progressPercent() {
        if (isFinished()) {
            return 100;
        }
        if (totalRows == null || totalRows == 0 || processedRows == null) {
            return 0;
        }
        return Math.min(100, processedRows * 100 / totalRows);
    }

    public void fail(String code, String message, LocalDateTime now) {
        status = ImportStatus.FAILED;
        errorCode = code;
        errorMessage = message;
        currentStep = null;
        completedAt = now;
    }
}
