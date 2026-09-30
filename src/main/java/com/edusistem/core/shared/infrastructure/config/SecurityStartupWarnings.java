package com.edusistem.core.shared.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Avisa al arrancar de la configuración que no debería llegar a producción (no la impide: sirve en desarrollo). */
@Component
public class SecurityStartupWarnings {

    private static final Logger log = LoggerFactory.getLogger(SecurityStartupWarnings.class);

    private final Environment env;
    private final StorageProperties storage;

    public SecurityStartupWarnings(Environment env, StorageProperties storage) {
        this.env = env;
        this.storage = storage;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warn() {
        if (env.getProperty("springdoc.api-docs.enabled", Boolean.class, false)) {
            log.warn("API docs are enabled (API_DOCS_ENABLED=true): disable them in production");
        }
        if (storage.encryptionKey() == null || storage.encryptionKey().isBlank()) {
            log.warn("Stored files are not encrypted (STORAGE_ENCRYPTION_KEY is empty): set it in production");
        }
        if ("none".equalsIgnoreCase(env.getProperty("server.forward-headers-strategy", "none"))) {
            log.info("FORWARD_HEADERS_STRATEGY=none: rate limits use the connection IP. Behind a reverse proxy set "
                    + "it to native, or every client will share the proxy's IP");
        }
    }
}
