package com.edusistem.core.auth.domain.vo;

public record IssuedToken(String accessToken, String tokenType, long expiresInSeconds) {
}
