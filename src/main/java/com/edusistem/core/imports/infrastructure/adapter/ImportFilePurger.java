package com.edusistem.core.imports.infrastructure.adapter;

import com.edusistem.core.imports.domain.inputports.PurgeImportFilesUseCase;
import com.edusistem.core.imports.infrastructure.config.ImportProperties;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Limpieza diaria de los archivos de importaciones terminadas hace más de edusistem.imports.file-retention-days. */
@Component
public class ImportFilePurger {

    private static final Logger log = LoggerFactory.getLogger(ImportFilePurger.class);

    private final PurgeImportFilesUseCase purge;
    private final ImportProperties properties;
    private final Clock clock;

    public ImportFilePurger(PurgeImportFilesUseCase purge, ImportProperties properties, Clock clock) {
        this.purge = purge;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void purge() {
        int purged = purge.purgeFilesCompletedBefore(
                LocalDateTime.now(clock).minusDays(properties.fileRetentionDaysOrDefault()));
        if (purged > 0) {
            log.info("Purged the files of {} finished imports", purged);
        }
    }
}
