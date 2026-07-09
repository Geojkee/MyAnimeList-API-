package com.dwtd.myanimelist.features.auth.service;

import com.dwtd.myanimelist.exception.token.InvalidRefreshTokenException;
import com.dwtd.myanimelist.exception.token.TokenNotFoundException;
import com.dwtd.myanimelist.features.auth.entity.RefreshToken;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.refresh-token.duration-ms}")
    private long refreshTokenDuration;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .createdAt(Instant.now())
                .expiryDate(Instant.now().plusMillis(refreshTokenDuration))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(TokenNotFoundException::new);

        if (!refreshToken.isValid()) {
            throw new InvalidRefreshTokenException();
        }

        log.debug("Refresh token validated successfully for user: {}", refreshToken.getUser().getUsername());
        return refreshToken;
    }

    @Transactional
    public void revokeAllUsersToken(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Revoked all refresh tokens for user id: {}", userId);
    }

    @Transactional
    public void revokeToken(String token) {
        log.info("Attempting to revoke token: {}", token);
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            log.info("Found token with id: {}, revoked at: {}", refreshToken.getId(), refreshToken.getRevokedAt());
            refreshToken.setRevokedAt(Instant.now());
            refreshTokenRepository.save(refreshToken);
            log.info("Token revoked, saved with revokedAt: {}", refreshToken.getRevokedAt());
        });
    }
}
