package com.dwtd.myanimelist.features.auth.service;

import com.dwtd.myanimelist.exception.user.EmailAlreadyExistsException;
import com.dwtd.myanimelist.exception.user.UserNotFoundException;
import com.dwtd.myanimelist.exception.user.UsernameAlreadyExistsException;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User create(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new UsernameAlreadyExistsException(user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException(user.getEmail());
        }

        User savedUser = save(user);
        log.info("User created: username={}, email={}", savedUser.getUsername(), savedUser.getEmail());
        return savedUser;
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        var username = authentication.getName();

        return getByUsername(username);
    }
}