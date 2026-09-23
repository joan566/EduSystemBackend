package com.edusistem.core.user.infrastructure.adapter;

import com.edusistem.core.authorization.domain.enums.RoleName;
import com.edusistem.core.authorization.infrastructure.entity.RoleEntity;
import com.edusistem.core.authorization.infrastructure.repository.SpringDataRoleRepository;
import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import com.edusistem.core.user.domain.vo.AuthState;
import com.edusistem.core.user.infrastructure.entity.UserEntity;
import com.edusistem.core.user.infrastructure.entity.UserRoleEntity;
import com.edusistem.core.user.infrastructure.mapper.UserMapper;
import com.edusistem.core.user.infrastructure.repository.SpringDataUserRepository;
import com.edusistem.core.user.infrastructure.repository.SpringDataUserRoleRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository users;
    private final SpringDataUserRoleRepository userRoles;
    private final SpringDataRoleRepository roles;
    private final UserMapper mapper;

    public UserRepositoryAdapter(SpringDataUserRepository users, SpringDataUserRoleRepository userRoles,
                                 SpringDataRoleRepository roles, UserMapper mapper) {
        this.users = users;
        this.userRoles = userRoles;
        this.roles = roles;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity saved = users.save(mapper.toEntity(user));
        syncRoles(saved.getId(), user.getRoles());
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return users.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.existsByEmail(email);
    }

    @Override
    public Optional<AuthState> findAuthState(Long id) {
        return users.findAuthState(id);
    }

    private User toDomain(UserEntity entity) {
        User user = mapper.toDomain(entity);
        Set<RoleName> names = userRoles.findRoleNamesByUserId(entity.getId()).stream()
                .map(RoleName::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleName.class)));
        user.setRoles(names);
        return user;
    }

    private void syncRoles(Long userId, Set<RoleName> desired) {
        Set<String> current = Set.copyOf(userRoles.findRoleNamesByUserId(userId));
        List<RoleEntity> all = roles.findAll();
        for (RoleEntity role : all) {
            boolean wanted = desired.stream().anyMatch(r -> r.name().equals(role.getName()));
            if (wanted && !current.contains(role.getName())) {
                userRoles.save(new UserRoleEntity(userId, role.getId()));
            }
        }
    }
}
