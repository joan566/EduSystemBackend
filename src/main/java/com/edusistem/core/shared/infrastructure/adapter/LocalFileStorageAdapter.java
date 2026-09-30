package com.edusistem.core.shared.infrastructure.adapter;

import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.shared.infrastructure.config.StorageProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Archivos en disco local. Con {@code edusistem.storage.encryption-key} se guardan cifrados con AES-256-GCM:
 * cabecera {@code EDUENC1}, IV de 12 bytes y el texto cifrado con su etiqueta; la ruta relativa va como dato
 * autenticado, así que un archivo movido o manipulado no se descifra. Los archivos sin cabecera (guardados antes de
 * activar el cifrado) se siguen leyendo tal cual.
 */
@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private static final byte[] MAGIC = "EDUENC1\0".getBytes(StandardCharsets.US_ASCII);
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path base;
    private final SecretKey key;

    public LocalFileStorageAdapter(StorageProperties properties) {
        this.base = Path.of(properties.basePath()).toAbsolutePath().normalize();
        this.key = parseKey(properties.encryptionKey());
    }

    @Override
    public String store(String directory, String fileName, byte[] content) {
        String safeName = UUID.randomUUID() + "-" + sanitize(fileName);
        Path target = resolve(directory + "/" + safeName);
        String relative = base.relativize(target).toString();
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, key == null ? content : encrypt(content, relative));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file " + safeName, e);
        }
        return relative;
    }

    @Override
    public byte[] read(String relativePath) throws IOException {
        Path path = resolve(relativePath);
        byte[] stored = Files.readAllBytes(path);
        if (!isEncrypted(stored)) {
            return stored;
        }
        if (key == null) {
            throw new IOException("File " + relativePath + " is encrypted but no storage encryption key is configured");
        }
        return decrypt(stored, base.relativize(path).toString());
    }

    @Override
    public boolean exists(String relativePath) {
        return Files.exists(resolve(relativePath));
    }

    @Override
    public void delete(String relativePath) throws IOException {
        Files.deleteIfExists(resolve(relativePath));
    }

    private Path resolve(String relativePath) {
        Path resolved = base.resolve(relativePath).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("Path escapes the storage directory");
        }
        return resolved;
    }

    private byte[] encrypt(byte[] content, String relativePath) {
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(relativePath.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(content);
            return ByteBuffer.allocate(MAGIC.length + IV_LENGTH + encrypted.length).put(MAGIC).put(iv).put(encrypted)
                    .array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt file", e);
        }
    }

    private byte[] decrypt(byte[] stored, String relativePath) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, stored, MAGIC.length, IV_LENGTH));
            cipher.updateAAD(relativePath.getBytes(StandardCharsets.UTF_8));
            int offset = MAGIC.length + IV_LENGTH;
            return cipher.doFinal(stored, offset, stored.length - offset);
        } catch (GeneralSecurityException e) {
            throw new IOException("Could not decrypt file " + relativePath + " (wrong key or tampered file)", e);
        }
    }

    private static boolean isEncrypted(byte[] stored) {
        return stored.length >= MAGIC.length + IV_LENGTH
                && Arrays.equals(stored, 0, MAGIC.length, MAGIC, 0, MAGIC.length);
    }

    private static SecretKey parseKey(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("STORAGE_ENCRYPTION_KEY must be Base64 (e.g. openssl rand -base64 32)");
        }
        if (bytes.length != 32) {
            throw new IllegalStateException("STORAGE_ENCRYPTION_KEY must decode to exactly 32 bytes (AES-256)");
        }
        return new SecretKeySpec(bytes, "AES");
    }

    private static String sanitize(String name) {
        return name == null ? "file" : name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
