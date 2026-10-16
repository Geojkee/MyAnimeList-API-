package com.dwtd.myanimelist.features.auth.service;

import com.dwtd.myanimelist.exception.token.InvalidRefreshTokenException;
import com.dwtd.myanimelist.exception.token.TokenNotFoundException;
import com.dwtd.myanimelist.features.auth.entity.RefreshToken;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.repository.RefreshTokenRepository;
import com.dwtd.myanimelist.helpers.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDuration", 60_000L);
    }

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

    @Test
    void validateRefreshToken_ShouldThrow_WhenTokenRevoked() {
        RefreshToken revoked = RefreshToken.builder()
                .token("revoked")
                .expiryDate(Instant.now().plusSeconds(60))
                .revokedAt(Instant.now())
                .build();
        when(refreshTokenRepository.findByToken("revoked")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("revoked"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateRefreshToken_ShouldReturnToken_WhenValid() {
        User user = TestDataFactory.user("Alice", "alice@example.com", Role.ROLE_USER);
        RefreshToken token = RefreshToken.builder()
                .token("valid")
                .user(user)
                .expiryDate(Instant.now().plusSeconds(60))
                .revokedAt(null)
                .build();
        when(refreshTokenRepository.findByToken("valid")).thenReturn(Optional.of(token));

        assertThat(refreshTokenService.validateRefreshToken("valid")).isEqualTo(token);
    }

    @Test
    void createRefreshToken_ShouldReplaceExistingAndSave() {
        User user = User.builder().id(1L).username("Alice").email("a@a.com").role(Role.ROLE_USER).build();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken created = refreshTokenService.createRefreshToken(user);

        verify(refreshTokenRepository).deleteByUserId(1L);
        assertThat(created.getToken()).isNotBlank();
        assertThat(created.getUser()).isEqualTo(user);
        assertThat(created.getExpiryDate()).isAfter(Instant.now());
    }

    @Test
    void revokeToken_ShouldSetRevokedAt_WhenFound() {
        RefreshToken token = RefreshToken.builder()
                .id(5L)
                .token("to-revoke")
                .expiryDate(Instant.now().plusSeconds(60))
                .build();
        when(refreshTokenRepository.findByToken("to-revoke")).thenReturn(Optional.of(token));

        refreshTokenService.revokeToken("to-revoke");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getRevokedAt()).isNotNull();
    }

    @Test
    void revokeToken_ShouldDoNothing_WhenMissing() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        refreshTokenService.revokeToken("missing");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeAllUsersToken_ShouldDeleteByUserId() {
        refreshTokenService.revokeAllUsersToken(7L);

        verify(refreshTokenRepository).deleteByUserId(7L);
    }
}
