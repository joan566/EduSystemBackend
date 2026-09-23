package com.edusistem.core.auth.infrastructure.adapter;

import com.edusistem.core.auth.domain.entity.RefreshToken;
import com.edusistem.core.auth.domain.outputports.RefreshTokenRepositoryPort;
import com.edusistem.core.auth.infrastructure.mapper.RefreshTokenMapper;
import com.edusistem.core.auth.infrastructure.repository.SpringDataRefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final SpringDataRefreshTokenRepository repository;
    private final RefreshTokenMapper mapper;

    public RefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository repository, RefreshTokenMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return mapper.toDomain(repository.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(Long userId, LocalDateTime now) {
        repository.revokeAllByUserId(userId, now);
    }
}
