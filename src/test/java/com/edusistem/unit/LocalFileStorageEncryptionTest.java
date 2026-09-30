package com.edusistem.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edusistem.core.shared.infrastructure.adapter.LocalFileStorageAdapter;
import com.edusistem.core.shared.infrastructure.config.StorageProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageEncryptionTest {

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String OTHER_KEY = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final byte[] CONTENT = "hoja de respuestas de Ana".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path dir;

    private LocalFileStorageAdapter storage(String key) {
        return new LocalFileStorageAdapter(new StorageProperties(dir.toString(), key));
    }

    @Test
    void storesEncryptedAndReadsBackThePlainContent() throws IOException {
        LocalFileStorageAdapter storage = storage(KEY);
        String path = storage.store("submissions/exam-1", "sheet.jpg", CONTENT);
        byte[] onDisk = Files.readAllBytes(dir.resolve(path));
        assertThat(onDisk).startsWith("EDUENC1".getBytes(StandardCharsets.US_ASCII));
        assertThat(new String(onDisk, StandardCharsets.ISO_8859_1)).doesNotContain("hoja de respuestas");
        assertThat(storage.read(path)).isEqualTo(CONTENT);
    }

    @Test
    void stillReadsFilesStoredBeforeEncryptionWasEnabled() throws IOException {
        String path = storage(null).store("imports", "students.xlsx", CONTENT);
        assertThat(storage(KEY).read(path)).isEqualTo(CONTENT);
    }

    @Test
    void refusesAWrongKeyAMissingKeyATamperedFileAndAMovedFile() throws IOException {
        String path = storage(KEY).store("submissions/exam-1", "sheet.jpg", CONTENT);
        assertThatThrownBy(() -> storage(OTHER_KEY).read(path)).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> storage(null).read(path)).isInstanceOf(IOException.class);

        Path moved = dir.resolve("submissions/exam-2/moved.jpg");
        Files.createDirectories(moved.getParent());
        Files.copy(dir.resolve(path), moved);
        assertThatThrownBy(() -> storage(KEY).read("submissions/exam-2/moved.jpg")).isInstanceOf(IOException.class);

        byte[] stored = Files.readAllBytes(dir.resolve(path));
        stored[stored.length - 1] ^= 1;
        Files.write(dir.resolve(path), stored);
        assertThatThrownBy(() -> storage(KEY).read(path)).isInstanceOf(IOException.class);
    }

    @Test
    void rejectsKeysThatAreNotAes256() {
        assertThatThrownBy(() -> storage("c2hvcnQ=")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> storage("not base64!")).isInstanceOf(IllegalStateException.class);
    }
}
