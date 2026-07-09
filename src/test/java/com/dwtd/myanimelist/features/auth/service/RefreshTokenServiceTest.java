package com.dwtd.myanimelist.features.auth.service;

import com.dwtd.myanimelist.exception.token.InvalidRefreshTokenException;
import com.dwtd.myanimelist.exception.token.TokenNotFoundException;
import com.dwtd.myanimelist.features.auth.entity.RefreshToken;
import com.dwtd.myanimelist.features.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {


    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void validateRefreshToken_ShouldThrowTokenNotFoundException_WhenTokenNotExist(){
        String invalidToken = "non-existent-token";

        when(refreshTokenRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(invalidToken)).isInstanceOf(TokenNotFoundException.class);
    }

    @Test
    void validateRefreshToken_ShouldThrowInvalidRefreshTokenException_WhenTokenExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .token("existing-token")
                .expiryDate(Instant.now().minusSeconds(60))
                .revokedAt(null)
                .build();

        when(refreshTokenRepository.findByToken("existing-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("existing-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
