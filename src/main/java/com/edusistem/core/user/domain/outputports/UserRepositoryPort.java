package com.edusistem.core.user.domain.outputports;

import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.domain.vo.AuthState;
import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Consulta liviana usada en cada petición autenticada para validar el JWT. */
    Optional<AuthState> findAuthState(Long id);
}
