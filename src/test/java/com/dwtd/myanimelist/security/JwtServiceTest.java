package com.dwtd.myanimelist.security;

import com.dwtd.myanimelist.features.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void SetUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "mySuperSecretKeyThatIsAtLeast32CharactersLong12345");
        ReflectionTestUtils.setField(jwtService, "token_expiration_MS", 86400000L);
    }

    @Test
    void generateToken_ShouldReturnJwtString() {
        User user = User.builder()
                .id(1L)
                .username("TestUser")
                .email("test@example.com")
                .build();

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();

        String extractedUsername = jwtService.extractUsername(token);

        assertThat(extractedUsername).isEqualTo(user.getUsername());
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        User user = User.builder()
                .username("TestUser")
                .build();

        String token = jwtService.generateToken(user);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("TestUser");
    }

    @Test
    void extractRole() {
        User user = User.builder()
                .username("TestUser")
                .build();

        String token = jwtService.generateToken(user);

        String role = jwtService.extractRole(token);
        assertThat(role).isEqualTo("ROLE_USER");
    }

    @Test
    void isTokenValid_ShouldReturnTrue_ForValidToken() {
        User user = User.builder()
                .username("TestUser")
                .build();

        String token = jwtService.generateToken(user);

        boolean isValid = jwtService.isTokenValid(token, user.getUsername());

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenUsernameMismatch() {
        User user1 = User.builder()
                .username("TestUser1")
                .build();

        User user2 = User.builder()
                .username("TestUser2")
                .build();

        String token = jwtService.generateToken(user1);

        boolean isValid = jwtService.isTokenValid(token, user2.getUsername());

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenTokenExpired() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "token_expiration_MS", 1000L);
        User user = User.builder()
                .username("TestUser")
                .build();

        String token = jwtService.generateToken(user);

        Thread.sleep(1500);

        boolean isValid = jwtService.isTokenValid(token, user.getUsername());

        assertThat(isValid).isFalse();
    }
}
