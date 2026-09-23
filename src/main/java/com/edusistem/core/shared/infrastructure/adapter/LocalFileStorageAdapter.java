package com.edusistem.core.shared.infrastructure.adapter;

import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.shared.infrastructure.config.StorageProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private final Path base;

    public LocalFileStorageAdapter(StorageProperties properties) {
        this.base = Path.of(properties.basePath()).toAbsolutePath().normalize();
    }

    @Override
    public String store(String directory, String fileName, byte[] content) {
        String safeName = UUID.randomUUID() + "-" + sanitize(fileName);
        Path target = resolve(directory + "/" + safeName);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file " + safeName, e);
        }
        return base.relativize(target).toString();
    }

    @Override
    public byte[] read(String relativePath) throws IOException {
        return Files.readAllBytes(resolve(relativePath));
    }

    @Override
    public boolean exists(String relativePath) {
        return Files.exists(resolve(relativePath));
    }

    private Path resolve(String relativePath) {
        Path resolved = base.resolve(relativePath).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("Path escapes the storage directory");
        }
        return resolved;
    }

    private static String sanitize(String name) {
        return name == null ? "file" : name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
