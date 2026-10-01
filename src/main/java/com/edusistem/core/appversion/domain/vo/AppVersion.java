package com.edusistem.core.appversion.domain.vo;

import java.util.Comparator;
import java.util.regex.Pattern;

/** Versión semántica {@code major.minor.patch} de la app móvil; se compara numéricamente (1.10.0 > 1.9.0). */
public record AppVersion(int major, int minor, int patch) implements Comparable<AppVersion> {

    private static final Pattern FORMAT = Pattern.compile("\\d{1,9}\\.\\d{1,9}\\.\\d{1,9}");
    private static final Comparator<AppVersion> ORDER = Comparator.comparingInt(AppVersion::major)
            .thenComparingInt(AppVersion::minor).thenComparingInt(AppVersion::patch);

    public AppVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version numbers must not be negative");
        }
    }

    /** Acepta solo el formato estricto {@code X.Y.Z} (sin prefijo "v" ni sufijo "+build"). */
    public static AppVersion parse(String value) {
        if (value == null || !FORMAT.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Invalid app version '" + value + "': expected format X.Y.Z");
        }
        String[] parts = value.trim().split("\\.");
        return new AppVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    public boolean isOlderThan(AppVersion other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(AppVersion other) {
        return ORDER.compare(this, other);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
