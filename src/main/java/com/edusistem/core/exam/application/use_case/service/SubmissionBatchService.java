package com.edusistem.core.exam.application.use_case.service;

import com.edusistem.core.exam.application.use_case.dtos.SubmissionCommands;
import com.edusistem.core.exam.application.use_case.service.ExamContextLoader.ExamContext;
import com.edusistem.core.exam.domain.entity.SubmissionBatch;
import com.edusistem.core.exam.domain.entity.SubmissionBatchPage;
import com.edusistem.core.exam.domain.enums.SubmissionBatchStatus;
import com.edusistem.core.exam.domain.inputports.ProcessSubmissionBatchUseCase;
import com.edusistem.core.exam.domain.inputports.PurgeSubmissionBatchFilesUseCase;
import com.edusistem.core.exam.domain.inputports.QuerySubmissionBatchUseCase;
import com.edusistem.core.exam.domain.inputports.SubmitAnswerSheetBatchUseCase;
import com.edusistem.core.exam.domain.outputports.BackgroundTaskPort;
import com.edusistem.core.exam.domain.outputports.ExamSubmissionRepositoryPort;
import com.edusistem.core.exam.domain.outputports.ScannedPdfPort;
import com.edusistem.core.exam.domain.outputports.SubmissionBatchRepositoryPort;
import com.edusistem.core.exam.domain.vo.ExamSubmissionSummary;
import com.edusistem.core.exam.domain.vo.SubmissionBatchDetails;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recibe el PDF con todas las hojas escaneadas y lo encola. Lo que invalida el lote entero (examen ajeno, preguntas
 * incompletas, sin escala, PDF ilegible o demasiado largo) se rechaza aquí, de forma síncrona; el resto se resuelve
 * página a página en {@link SubmissionBatchWorker}.
 */
public class SubmissionBatchService implements SubmitAnswerSheetBatchUseCase, QuerySubmissionBatchUseCase,
        PurgeSubmissionBatchFilesUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubmissionBatchService.class);

    static final int MAX_PAGES = 200;

    private final ExamContextLoader loader;
    private final SubmissionBatchRepositoryPort batches;
    private final ExamSubmissionRepositoryPort submissions;
    private final ScannedPdfPort scannedPdf;
    private final FileStoragePort storage;
    private final BackgroundTaskPort background;
    private final ProcessSubmissionBatchUseCase worker;
    private final OwnershipGuard guard;
    private final Clock clock;

    public SubmissionBatchService(ExamContextLoader loader, SubmissionBatchRepositoryPort batches,
                                  ExamSubmissionRepositoryPort submissions, ScannedPdfPort scannedPdf,
                                  FileStoragePort storage, BackgroundTaskPort background,
                                  ProcessSubmissionBatchUseCase worker, OwnershipGuard guard, Clock clock) {
        this.loader = loader;
        this.batches = batches;
        this.submissions = submissions;
        this.scannedPdf = scannedPdf;
        this.storage = storage;
        this.background = background;
        this.worker = worker;
        this.guard = guard;
        this.clock = clock;
    }

    @Override
    public SubmissionBatch start(SubmissionCommands.SubmitBatch command) {
        ExamContext ctx = loader.load(command.teacherId(), command.examId());
        if (!ctx.exam().isReady()) {
            throw new BusinessRuleException("EXAM_NOT_READY", "The exam questions are not fully defined");
        }
        loader.requireScale(ctx.evaluation().getTeachingPeriodId());
        int pages = scannedPdf.pageCount(command.pdf(), MAX_PAGES);
        String fileName = command.fileName() == null || command.fileName().isBlank() ? "scan.pdf" : command.fileName();
        String path = storage.store("submission-batches/exam-" + command.examId(), fileName, command.pdf());

        SubmissionBatch batch = batches.save(SubmissionBatch.builder().examId(command.examId())
                .teacherId(command.teacherId()).fileName(fileName).filePath(path)
                .replaceExisting(command.replace()).status(SubmissionBatchStatus.QUEUED).totalPages(pages)
                .processedPages(0).createdAt(LocalDateTime.now(clock)).build());
        Long batchId = batch.getId();
        background.run(() -> worker.process(batchId));
        return batch;
    }

    @Override
    public SubmissionBatchDetails get(Long teacherId, Long examId, Long batchId) {
        guard.requireExam(teacherId, examId);
        SubmissionBatch batch = batches.findById(batchId).filter(b -> b.getExamId().equals(examId))
                .orElseThrow(() -> ResourceNotFoundException.of("SubmissionBatch", batchId));
        List<SubmissionBatchPage> pages = batches.findPages(batchId);
        List<Long> ids = pages.stream().map(SubmissionBatchPage::getSubmissionId).filter(Objects::nonNull).toList();
        Map<Long, ExamSubmissionSummary> byId = submissions.findSummariesByIds(ids).stream()
                .collect(Collectors.toMap(ExamSubmissionSummary::id, Function.identity()));
        return new SubmissionBatchDetails(batch, pages, byId);
    }

    @Override
    public List<SubmissionBatch> list(Long teacherId, Long examId) {
        guard.requireExam(teacherId, examId);
        return batches.findByExamId(examId);
    }

    @Override
    public int purgeFilesCompletedBefore(LocalDateTime cutoff) {
        int deleted = 0;
        for (SubmissionBatch batch : batches.findFinishedWithFileBefore(cutoff)) {
            try {
                storage.delete(batch.getFilePath());
            } catch (IOException e) { // se reintenta en la siguiente ejecución
                log.warn("Could not delete the PDF of submission batch {}", batch.getId(), e);
                continue;
            }
            batch.filePurged(LocalDateTime.now(clock));
            batches.save(batch);
            deleted++;
        }
        return deleted;
    }
}
