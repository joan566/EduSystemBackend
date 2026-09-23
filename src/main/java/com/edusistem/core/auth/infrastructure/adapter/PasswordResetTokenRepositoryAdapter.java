package com.edusistem.core.auth.infrastructure.adapter;

import com.edusistem.core.auth.domain.entity.PasswordResetToken;
import com.edusistem.core.auth.domain.outputports.PasswordResetTokenRepositoryPort;
import com.edusistem.core.auth.infrastructure.mapper.PasswordResetTokenMapper;
import com.edusistem.core.auth.infrastructure.repository.SpringDataPasswordResetTokenRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepositoryPort {

    private final SpringDataPasswordResetTokenRepository repository;
    private final PasswordResetTokenMapper mapper;

    public PasswordResetTokenRepositoryAdapter(SpringDataPasswordResetTokenRepository repository,
                                               PasswordResetTokenMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return mapper.toDomain(repository.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<PasswordResetToken> findLatestByUserId(Long userId) {
        return repository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId).map(mapper::toDomain);
    }
}
