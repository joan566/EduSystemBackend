package com.edusistem.core.exam.application.use_case.service;

import com.edusistem.core.exam.application.use_case.dtos.SubmissionCommands;
import com.edusistem.core.exam.domain.entity.Exam;
import com.edusistem.core.exam.domain.entity.SubmissionBatch;
import com.edusistem.core.exam.domain.entity.SubmissionBatchPage;
import com.edusistem.core.exam.domain.enums.BatchPageOutcome;
import com.edusistem.core.exam.domain.exceptions.AnswerSheetProcessingException;
import com.edusistem.core.exam.domain.inputports.ProcessSubmissionBatchUseCase;
import com.edusistem.core.exam.domain.inputports.SubmitAnswerSheetUseCase;
import com.edusistem.core.exam.domain.outputports.AnswerSheetProcessorPort;
import com.edusistem.core.exam.domain.outputports.BackgroundTaskPort;
import com.edusistem.core.exam.domain.outputports.ScannedPdfPort;
import com.edusistem.core.exam.domain.outputports.SubmissionBatchRepositoryPort;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import com.edusistem.core.exam.domain.vo.QrPayload;
import com.edusistem.core.shared.domain.exceptions.DomainException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Califica en segundo plano las páginas de un lote: cada una pasa por el mismo flujo que una foto individual
 * ({@link SubmitAnswerSheetUseCase}); la nota y el registro de la página se guardan juntos
 * ({@link SubmissionBatchPageRecorder}), de modo que el progreso es consultable y un reinicio reanuda desde la primera
 * página sin registrar.
 */
public class SubmissionBatchWorker implements ProcessSubmissionBatchUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubmissionBatchWorker.class);

    private final SubmissionBatchRepositoryPort batches;
    private final ExamContextLoader loader;
    private final SubmissionBatchPageRecorder recorder;
    private final AnswerSheetProcessorPort processor;
    private final ScannedPdfPort scannedPdf;
    private final FileStoragePort storage;
    private final BackgroundTaskPort background;
    private final Clock clock;

    public SubmissionBatchWorker(SubmissionBatchRepositoryPort batches, ExamContextLoader loader,
                                 SubmissionBatchPageRecorder recorder, AnswerSheetProcessorPort processor,
                                 ScannedPdfPort scannedPdf, FileStoragePort storage, BackgroundTaskPort background,
                                 Clock clock) {
        this.batches = batches;
        this.loader = loader;
        this.recorder = recorder;
        this.processor = processor;
        this.scannedPdf = scannedPdf;
        this.storage = storage;
        this.background = background;
        this.clock = clock;
    }

    @Override
    public void resumeUnfinished() {
        for (SubmissionBatch batch : batches.findUnfinished()) {
            log.info("Resuming submission batch {} from page {}", batch.getId(), batch.getProcessedPages() + 1);
            background.run(() -> process(batch.getId()));
        }
    }

    @Override
    public void process(Long batchId) {
        SubmissionBatch batch = batches.findById(batchId).orElse(null);
        if (batch == null || batch.isFinished()) {
            return;
        }
        batch.start(now());
        batch = batches.save(batch);
        SubmissionBatch current = batch;
        try {
            Exam exam = loader.load(batch.getTeacherId(), batch.getExamId()).exam();
            AnswerSheetLayout layout = new AnswerSheetLayout(exam.getNumberOfQuestions(), exam.optionCount());
            byte[] pdf = storage.read(batch.getFilePath());
            // se reanuda según las páginas registradas (processedPages podría ir una por detrás si se cortó justo antes)
            List<SubmissionBatchPage> done = batches.findPages(batchId);
            Map<String, Integer> pageByStudentCode = new HashMap<>();
            done.stream().filter(p -> p.getStudentCode() != null)
                    .forEach(p -> pageByStudentCode.putIfAbsent(p.getStudentCode(), p.getPageNumber()));
            int nextPage = done.stream().mapToInt(SubmissionBatchPage::getPageNumber).max().orElse(0) + 1;
            current.setProcessedPages(done.size());
            scannedPdf.forEachPage(pdf, nextPage, (page, image) -> {
                processPage(current, layout, pageByStudentCode, page, image);
                current.pageProcessed();
                batches.save(current);
            });
            current.complete(now());
        } catch (DomainException e) {
            current.fail(e.getMessage(), now());
        } catch (IOException e) {
            log.error("Could not read the stored PDF of submission batch {}", batchId, e);
            current.fail("The uploaded PDF could not be read", now());
        } catch (RuntimeException e) {
            log.error("Unexpected error processing submission batch {}", batchId, e);
            current.fail("Unexpected error while processing the file", now());
        }
        batches.save(current);
    }

    /** Resuelve la página y la registra en el lote. */
    private void processPage(SubmissionBatch batch, AnswerSheetLayout layout, Map<String, Integer> pageByStudentCode,
                             int page, byte[] image) {
        SubmissionBatchPage result = SubmissionBatchPage.builder().batchId(batch.getId()).pageNumber(page).build();
        if (resolveWithoutSubmission(batch, layout, pageByStudentCode, page, image, result)) {
            batches.savePage(result);
            return;
        }
        try {
            recorder.submitAndRecord(new SubmissionCommands.Submit(batch.getTeacherId(), batch.getExamId(), image,
                    pageFileName(batch.getFileName(), page), null, batch.isReplaceExisting()), result);
        } catch (DomainException e) {
            batches.savePage(rejected(result, e.getCode(), e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Unexpected error processing page {} of submission batch {}", page, batch.getId(), e);
            batches.savePage(rejected(result, "PROCESSING_ERROR", "Unexpected error while processing the page"));
        }
    }

    /**
     * Resuelve las páginas que no llegan a calificarse (ilegible, sin QR, no es hoja, duplicada) y devuelve true;
     * si hay que calificarla, deja el código del estudiante en {@code result} y devuelve false.
     */
    private boolean resolveWithoutSubmission(SubmissionBatch batch, AnswerSheetLayout layout,
                                             Map<String, Integer> pageByStudentCode, int page, byte[] image,
                                             SubmissionBatchPage result) {
        if (image == null) {
            rejected(result, "PAGE_UNREADABLE", "The page could not be converted to an image");
            return true;
        }
        String qrText;
        try {
            qrText = processor.readQrCode(image).orElse(null);
        } catch (DomainException e) {
            rejected(result, e.getCode(), e.getMessage());
            return true;
        }
        if (qrText == null) {
            if (!looksLikeAnswerSheet(image, layout)) {
                result.setOutcome(BatchPageOutcome.SKIPPED);
                result.setMessage("The page is not an answer sheet");
            } else {
                rejected(result, "QR_NOT_DETECTED",
                        "No QR code was detected; upload this sheet individually providing studentId");
            }
            return true;
        }
        String studentCode = studentCodeOrNull(qrText);
        result.setStudentCode(studentCode);
        if (studentCode != null) {
            Integer firstPage = pageByStudentCode.putIfAbsent(studentCode, page);
            if (firstPage != null) {
                rejected(result, "DUPLICATE_IN_BATCH",
                        "Student " + studentCode + " already appears on page " + firstPage + " of this file");
                return true;
            }
        }
        return false;
    }

    /** Sin QR: si tampoco hay marcadores de esquina, no es una hoja (p. ej. el cuadernillo o una página en blanco). */
    private boolean looksLikeAnswerSheet(byte[] image, AnswerSheetLayout layout) {
        try {
            processor.readBubbles(image, layout);
            return true;
        } catch (AnswerSheetProcessingException e) {
            return false;
        }
    }

    private static String studentCodeOrNull(String qrText) {
        try {
            return QrPayload.parse(qrText).studentCode();
        } catch (DomainException e) {
            return null; // el flujo individual devuelve el error INVALID_QR
        }
    }

    private static String pageFileName(String fileName, int page) {
        String base = fileName == null || fileName.isBlank() ? "scan" : fileName.replaceFirst("\\.[^.]*$", "");
        return base + "-p" + page + ".jpg";
    }

    private static SubmissionBatchPage rejected(SubmissionBatchPage page, String code, String message) {
        page.setOutcome(BatchPageOutcome.REJECTED);
        page.setSubmissionId(null); // si el recorder falló, su submission se revirtió
        page.setErrorCode(code);
        page.setMessage(message);
        return page;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
