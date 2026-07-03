package com.dwtd.myanimelist.features.auth.service;

import com.dwtd.myanimelist.exception.InvalidRefreshTokenException;
import com.dwtd.myanimelist.features.auth.entity.RefreshToken;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.repository.RefreshTokenRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());

        long refreshTokenDuration = 30L * 1000 * 60 * 60 * 60 + 24;

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .createdAt(Instant.now())
                .expiryDate(Instant.now().plusMillis(refreshTokenDuration))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken validateRefreshToken(String token){
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token).orElseThrow();

        if (!refreshToken.isValid()) {
            throw new InvalidRefreshTokenException();
        }

        return refreshToken;
    }

    @Transactional
    public void revokeAllUsersToken(Long userId){
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Revoked all refresh tokens for user id: {}", userId);
    }

    @Transactional
    public void revokeToken(String token){
        log.info("Attempting to revoke token: {}", token);
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            log.info("Found token with id: {}, revoked at: {}", refreshToken.getId(), refreshToken.getRevokedAt());
            refreshToken.setRevokedAt(Instant.now());
            refreshTokenRepository.save(refreshToken);
            log.info("Token revoked, saved with revokedAt: {}", refreshToken.getRevokedAt());
        });
    }
}
