package com.edusistem.core.exam.infrastructure.adapter;

import com.edusistem.core.exam.domain.outputports.BackgroundTaskPort;
import com.edusistem.core.exam.infrastructure.config.SubmissionBatchProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Pool propio y acotado para los lotes de hojas (el OMR es intensivo en CPU/memoria). Al apagar no espera: los lotes a
 * medias se reanudan en el siguiente arranque ({@link SubmissionBatchRecovery}).
 */
@Component
public class SubmissionBatchExecutorAdapter implements BackgroundTaskPort, DisposableBean {

    private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    public SubmissionBatchExecutorAdapter(SubmissionBatchProperties properties) {
        executor.setCorePoolSize(properties.workersOrDefault());
        executor.setMaxPoolSize(properties.workersOrDefault());
        executor.setThreadNamePrefix("submission-batch-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
    }

    @Override
    public void run(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }
}
