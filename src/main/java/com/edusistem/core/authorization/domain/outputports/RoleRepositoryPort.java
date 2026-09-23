package com.edusistem.core.authorization.domain.outputports;

import com.edusistem.core.authorization.domain.entity.Role;
import com.edusistem.core.authorization.domain.enums.RoleName;
import java.util.Optional;

public interface RoleRepositoryPort {

    Optional<Role> findByName(RoleName name);
}
