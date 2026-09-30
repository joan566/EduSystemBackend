package com.edusistem.core.auth.domain.inputports;

/** Elimina la cuenta del profesor y todos sus datos (estudiantes, notas, archivos...). Requiere la contraseña. */
public interface DeleteAccountUseCase {

    void deleteAccount(Long userId, String password);
}
