package com.edusistem.core.imports.application.support;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Avance de UNA importación en curso: cuántas filas del archivo se recorrieron y qué hoja se procesa. Lo refleja en el
 * batch y lo guarda como mucho una vez por {@link #SAVE_INTERVAL} (y siempre al empezar o cambiar de hoja), para que
 * el cliente que consulta {@code GET /imports/{id}} vea la importación avanzar sin una escritura por fila.
 */
public final class ImportProgress {

    static final Duration SAVE_INTERVAL = Duration.ofSeconds(1);

    private static final Logger log = LoggerFactory.getLogger(ImportProgress.class);

    private final ImportBatch batch;
    private final ImportBatchRepositoryPort batches;
    private final Clock clock;
    private int total;
    private int processed;
    private String step;
    private Instant lastSaved;

    public ImportProgress(ImportBatch batch, ImportBatchRepositoryPort batches, Clock clock) {
        this.batch = batch;
        this.batches = batches;
        this.clock = clock;
    }

    /** Ya se leyó el archivo: {@code totalRows} filas por recorrer. */
    public void start(int totalRows) {
        total = totalRows;
        processed = 0;
        save();
    }

    /** Empieza la hoja {@code label} (nombre visible, en español). */
    public void step(String label) {
        step = label;
        save();
    }

    /** Se recorrió una fila más. */
    public void tick() {
        processed++;
        if (lastSaved == null || !clock.instant().isBefore(lastSaved.plus(SAVE_INTERVAL))) {
            save();
        }
    }

    private void save() {
        batch.setTotalRows(total);
        batch.setProcessedRows(Math.min(processed, total));
        batch.setCurrentStep(step);
        lastSaved = clock.instant();
        try {
            batches.updateProgress(batch.getId(), batch.getProcessedRows(), total, step);
        } catch (RuntimeException e) {
            // el avance es informativo: no detiene la importación
            log.warn("Could not save the progress of import batch {}", batch.getId(), e);
        }
    }
}
