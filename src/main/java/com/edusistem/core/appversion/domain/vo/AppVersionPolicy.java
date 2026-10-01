package com.edusistem.core.appversion.domain.vo;

import java.util.Objects;

/** Última versión publicada de la app y mínima permitida; por debajo de {@code minimum} la actualización es obligatoria. */
public record AppVersionPolicy(AppVersion latest, AppVersion minimum) {

    public AppVersionPolicy {
        Objects.requireNonNull(latest, "latest");
        Objects.requireNonNull(minimum, "minimum");
        if (latest.isOlderThan(minimum)) {
            throw new IllegalArgumentException(
                    "Minimum app version " + minimum + " must not be greater than latest version " + latest);
        }
    }
}
