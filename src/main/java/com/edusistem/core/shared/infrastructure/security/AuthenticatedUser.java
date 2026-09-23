package com.edusistem.core.shared.infrastructure.security;

import java.util.Set;

/** Principal derivado del JWT; es la única fuente del identificador del profesor en los casos de uso. */
public record AuthenticatedUser(Long id, String email, Set<String> roles) {
}
