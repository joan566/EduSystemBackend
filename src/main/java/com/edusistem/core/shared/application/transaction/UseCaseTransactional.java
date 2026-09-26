package com.edusistem.core.shared.application.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un caso de uso que debe ejecutarse en una única transacción. Es Java puro: la capa de infrastructure
 * ({@code shared/infrastructure/config/TransactionConfig}) la traduce a la gestión transaccional de Spring.
 * Por defecto se hace rollback ante cualquier RuntimeException o Error.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface UseCaseTransactional {

    /** Excepciones que NO deben provocar rollback. */
    Class<? extends Throwable>[] noRollbackFor() default {};
}
