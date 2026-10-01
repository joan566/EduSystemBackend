package com.edusistem.core.imports.infrastructure.adapter;

import com.edusistem.core.imports.domain.outputports.ImportBackgroundTaskPort;
import com.edusistem.core.imports.infrastructure.config.ImportProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Pool propio y acotado para las importaciones (separado del de los lotes de exámenes). Al apagar no espera: las
 * importaciones a medias se reanudan en el siguiente arranque ({@link ImportBatchRecovery}).
 */
@Component
public class ImportExecutorAdapter implements ImportBackgroundTaskPort, DisposableBean {

    private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    public ImportExecutorAdapter(ImportProperties properties) {
        executor.setCorePoolSize(properties.workersOrDefault());
        executor.setMaxPoolSize(properties.workersOrDefault());
        executor.setThreadNamePrefix("import-batch-");
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
