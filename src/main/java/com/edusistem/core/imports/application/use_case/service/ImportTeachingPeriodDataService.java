package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.imports.application.contracts.ImportQueue;
import com.edusistem.core.imports.application.contracts.ImportQueue.ImportRequest;
import com.edusistem.core.imports.application.teachingperiod.TeachingPeriodImportProcessor;
import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportType;
import com.edusistem.core.imports.domain.inputports.ImportTeachingPeriodDataUseCase;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;

/**
 * Comprueba que el teaching period exista y sea del profesor, y encola su Excel combinado (lo aplica
 * {@link TeachingPeriodImportProcessor}).
 */
public class ImportTeachingPeriodDataService implements ImportTeachingPeriodDataUseCase {

    private final ImportQueue queue;
    private final OwnershipGuard guard;
    private final TeachingPeriodRepositoryPort teachingPeriods;

    public ImportTeachingPeriodDataService(ImportQueue queue, OwnershipGuard guard,
                                           TeachingPeriodRepositoryPort teachingPeriods) {
        this.queue = queue;
        this.guard = guard;
        this.teachingPeriods = teachingPeriods;
    }

    @Override
    public ImportBatch importData(ImportCommands.ImportTeachingPeriodData command) {
        Long teachingPeriodId = command.teachingPeriodId();
        guard.requireTeachingPeriod(command.teacherId(), teachingPeriodId);
        teachingPeriods.findViewById(teachingPeriodId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", teachingPeriodId));
        return queue.submit(new ImportRequest(command.teacherId(), ImportType.TEACHING_PERIOD, teachingPeriodId,
                command.fileName(), "teaching-period.xlsx", command.content()));
    }
}
