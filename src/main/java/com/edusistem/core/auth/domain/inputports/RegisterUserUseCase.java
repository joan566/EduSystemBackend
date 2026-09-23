package com.edusistem.core.auth.domain.inputports;

import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.auth.application.use_case.dtos.AuthResult;

public interface RegisterUserUseCase {

    AuthResult register(AuthCommands.Register command);
}
