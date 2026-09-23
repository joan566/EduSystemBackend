package com.edusistem.core.user.domain.inputports;

import com.edusistem.core.user.domain.entity.User;

public interface GetCurrentUserUseCase {

    User getById(Long userId);
}
