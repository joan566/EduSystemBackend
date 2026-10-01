package com.edusistem.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.core.imports.application.support.ImportProgress;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportStatus;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ImportProgressTest {

    /** Guarda cada avance que se persiste. */
    private static final class RecordingBatches implements ImportBatchRepositoryPort {
        final List<String> saved = new ArrayList<>();

        @Override
        public void updateProgress(Long id, int processedRows, int totalRows, String currentStep) {
            saved.add(processedRows + "/" + totalRows + " " + currentStep);
        }

        @Override
        public ImportBatch save(ImportBatch batch) {
            return batch;
        }

        @Override
        public Optional<ImportBatch> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public PageResult<ImportBatch> findByUserId(Long userId, PageQuery page) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ImportBatch> findCompletedWithFilesBefore(LocalDateTime cutoff) {
            return List.of();
        }

        @Override
        public List<ImportBatch> findUnfinished() {
            return List.of();
        }

        @Override
        public boolean existsUnfinishedByUserId(Long userId) {
            return false;
        }

        @Override
        public void saveErrors(Long batchId, List<ImportRowError> errors) {
        }

        @Override
        public List<ImportRowError> findErrors(Long batchId) {
            return List.of();
        }
    }

    /** Reloj que solo avanza cuando el test lo pide. */
    private static final class ManualClock extends Clock {
        Instant now = Instant.parse("2026-10-01T10:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void savesOnStartAndOnEachSheetButThrottlesRows() {
        RecordingBatches batches = new RecordingBatches();
        ManualClock clock = new ManualClock();
        ImportBatch batch = ImportBatch.builder().id(1L).status(ImportStatus.PROCESSING).build();
        ImportProgress progress = new ImportProgress(batch, batches, clock);

        progress.start(4);
        progress.step("Grados");
        progress.tick();
        progress.tick();
        clock.now = clock.now.plus(Duration.ofSeconds(1));
        progress.tick();
        progress.step("Grupos");

        assertThat(batches.saved).containsExactly("0/4 null", "0/4 Grados", "3/4 Grados", "3/4 Grupos");
        assertThat(batch.getProcessedRows()).isEqualTo(3);
        assertThat(batch.getCurrentStep()).isEqualTo("Grupos");
        assertThat(batch.progressPercent()).isEqualTo(75);
    }

    @Test
    void aFinishedBatchIsAtOneHundredPercent() {
        ImportBatch batch = ImportBatch.builder().status(ImportStatus.FAILED).totalRows(10).processedRows(2).build();
        assertThat(batch.progressPercent()).isEqualTo(100);
        assertThat(ImportBatch.builder().status(ImportStatus.QUEUED).build().progressPercent()).isZero();
    }
}
