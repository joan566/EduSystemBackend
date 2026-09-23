package com.edusistem.core.authorization.infrastructure.adapter;

import com.edusistem.core.authorization.domain.entity.Role;
import com.edusistem.core.authorization.domain.enums.RoleName;
import com.edusistem.core.authorization.domain.outputports.RoleRepositoryPort;
import com.edusistem.core.authorization.infrastructure.mapper.RoleMapper;
import com.edusistem.core.authorization.infrastructure.repository.SpringDataRoleRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RoleRepositoryAdapter implements RoleRepositoryPort {

    private final SpringDataRoleRepository repository;
    private final RoleMapper mapper;

    public RoleRepositoryAdapter(SpringDataRoleRepository repository, RoleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Role> findByName(RoleName name) {
        return repository.findByName(name.name()).map(mapper::toDomain);
    }
}
