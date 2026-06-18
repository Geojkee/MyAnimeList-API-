package com.dwtd.myanimelist.features.auth.service;

import com.dwtd.myanimelist.features.auth.Enum.Role;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void save(User user){
        userRepository.save(user);
    }

    public void create(User user){
        if (userRepository.existsByUsername(user.getUsername())){
            throw new RuntimeException("User with this Username is already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())){
            throw new RuntimeException("User with this email is already exists");
        }

        save(user);
    }

    public User getByUsername(String username){
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public UserDetailsService userDetailsService(){
        return this::getByUsername;
    }

    public User getCurrentUser(){
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        return getByUsername(username);
    }

    public void getAdmin(){
        var user = getCurrentUser();
        user.setRole(Role.ROLE_ADMIN);
        save(user);
    }
}
