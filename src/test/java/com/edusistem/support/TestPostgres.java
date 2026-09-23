package com.edusistem.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;

/** PostgreSQL embebido compartido por toda la ejecución de tests (no requiere Docker ni una instancia instalada). */
public final class TestPostgres {

    private static final EmbeddedPostgres INSTANCE = start();

    private TestPostgres() {
    }

    private static EmbeddedPostgres start() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the embedded PostgreSQL", e);
        }
    }

    public static String url() {
        return INSTANCE.getJdbcUrl("postgres", "postgres");
    }

    public static String username() {
        return "postgres";
    }

    public static String password() {
        return "postgres";
    }
}
