package com.edusistem.core.exam.infrastructure.adapter;

import com.edusistem.core.exam.domain.inputports.PurgeSubmissionBatchFilesUseCase;
import com.edusistem.core.exam.infrastructure.config.SubmissionBatchProperties;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Limpieza diaria de los PDF de lotes terminados hace más de edusistem.submission-batches.file-retention-days. */
@Component
public class SubmissionBatchFilePurger {

    private static final Logger log = LoggerFactory.getLogger(SubmissionBatchFilePurger.class);

    private final PurgeSubmissionBatchFilesUseCase purge;
    private final SubmissionBatchProperties properties;
    private final Clock clock;

    public SubmissionBatchFilePurger(PurgeSubmissionBatchFilesUseCase purge, SubmissionBatchProperties properties,
                                     Clock clock) {
        this.purge = purge;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "0 45 3 * * *")
    public void purge() {
        int deleted = purge.purgeFilesCompletedBefore(
                LocalDateTime.now(clock).minusDays(properties.fileRetentionDaysOrDefault()));
        if (deleted > 0) {
            log.info("Purged {} scanned PDF files of finished submission batches", deleted);
        }
    }
}
