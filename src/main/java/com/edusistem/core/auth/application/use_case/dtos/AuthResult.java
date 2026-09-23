package com.edusistem.core.auth.application.use_case.dtos;

import com.edusistem.core.auth.domain.vo.IssuedToken;
import com.edusistem.core.user.domain.entity.User;

public record AuthResult(IssuedToken token, String refreshToken, User user) {
}
