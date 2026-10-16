package com.dwtd.myanimelist.features.auth.service;

import com.dwtd.myanimelist.exception.user.EmailAlreadyExistsException;
import com.dwtd.myanimelist.exception.user.UserNotFoundException;
import com.dwtd.myanimelist.exception.user.UsernameAlreadyExistsException;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.repository.UserRepository;
import com.dwtd.myanimelist.helpers.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldSaveUser_whenUsernameAndEmailAreFree() {
        User user = TestDataFactory.user("Alice", "alice@example.com", Role.ROLE_USER);
        when(userRepository.existsByUsername("Alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User saved = userService.create(user);

        assertThat(saved.getUsername()).isEqualTo("Alice");
        verify(userRepository).save(user);
    }

    @Test
    void create_shouldThrow_whenUsernameExists() {
        User user = TestDataFactory.user("Alice", "alice@example.com", Role.ROLE_USER);
        when(userRepository.existsByUsername("Alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(user))
                .isInstanceOf(UsernameAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_shouldThrow_whenEmailExists() {
        User user = TestDataFactory.user("Alice", "alice@example.com", Role.ROLE_USER);
        when(userRepository.existsByUsername("Alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(user))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getByUsername_shouldReturnUser_whenExists() {
        User user = TestDataFactory.user("Alice", "alice@example.com", Role.ROLE_USER);
        when(userRepository.findByUsername("Alice")).thenReturn(Optional.of(user));

        assertThat(userService.getByUsername("Alice")).isEqualTo(user);
    }

    @Test
    void getByUsername_shouldThrow_whenMissing() {
        when(userRepository.findByUsername("Missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByUsername("Missing"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getCurrentUser_shouldReturnAuthenticatedUser() {
        User user = TestDataFactory.user("Alice", "alice@example.com", Role.ROLE_USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("Alice", null, List.of())
        );
        when(userRepository.findByUsername("Alice")).thenReturn(Optional.of(user));

        assertThat(userService.getCurrentUser()).isEqualTo(user);
    }

    @Test
    void getCurrentUser_shouldThrow_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authenticated");
    }

    @Test
    void getCurrentUser_shouldThrow_whenAuthenticationIsNotAuthenticated() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("Alice", null);
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authenticated");
    }

    @Test
    void updateCurrentUser_shouldUpdateUsernameAndEmail_whenFree() {
        User user = User.builder()
                .id(1L)
                .username("Alice")
                .email("alice@example.com")
                .password("encoded")
                .role(Role.ROLE_USER)
                .build();
        authenticate("Alice");
        when(userRepository.findByUsername("Alice")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("AliceNew")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateCurrentUser("AliceNew", "new@example.com");

        assertThat(updated.getUsername()).isEqualTo("AliceNew");
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        verify(userRepository).save(user);
    }

    @Test
    void updateCurrentUser_shouldSkipDuplicateCheck_whenValuesUnchanged() {
        User user = User.builder()
                .id(1L)
                .username("Alice")
                .email("alice@example.com")
                .password("encoded")
                .role(Role.ROLE_USER)
                .build();
        authenticate("Alice");
        when(userRepository.findByUsername("Alice")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateCurrentUser("Alice", "alice@example.com");

        assertThat(updated.getUsername()).isEqualTo("Alice");
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void updateCurrentUser_shouldThrow_whenUsernameTaken() {
        User user = User.builder()
                .id(1L)
                .username("Alice")
                .email("alice@example.com")
                .password("encoded")
                .role(Role.ROLE_USER)
                .build();
        authenticate("Alice");
        when(userRepository.findByUsername("Alice")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("Bob")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateCurrentUser("Bob", null))
                .isInstanceOf(UsernameAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateCurrentUser_shouldThrow_whenEmailTaken() {
        User user = User.builder()
                .id(1L)
                .username("Alice")
                .email("alice@example.com")
                .password("encoded")
                .role(Role.ROLE_USER)
                .build();
        authenticate("Alice");
        when(userRepository.findByUsername("Alice")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateCurrentUser(null, "bob@example.com"))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }
}
