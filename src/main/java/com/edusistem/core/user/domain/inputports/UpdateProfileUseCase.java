package com.edusistem.core.user.domain.inputports;

import com.edusistem.core.user.application.use_case.dtos.UpdateProfileCommand;
import com.edusistem.core.user.domain.entity.User;

public interface UpdateProfileUseCase {

    User update(UpdateProfileCommand command);
}
