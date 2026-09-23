package com.edusistem.core.auth.domain.outputports;

import com.edusistem.core.auth.domain.vo.IssuedToken;
import com.edusistem.core.user.domain.entity.User;

public interface TokenIssuerPort {

    IssuedToken issue(User user);
}
