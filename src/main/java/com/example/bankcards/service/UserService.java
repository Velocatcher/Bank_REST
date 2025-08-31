package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }
    /** Регистрация обычного пользователя (ROLE_USER). */
    public User register(String username, String rawPassword){
        if (username == null || username.length() < 3) throw new BadRequestException("Username too short");
        if (rawPassword == null || rawPassword.length() < 6) throw new BadRequestException("Password too short");
        if (repo.existsByUsername(username)) throw new BadRequestException("Username taken");
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(rawPassword));
        u.setRoles(Set.of(Role.USER));
        return repo.save(u);
    }

    /** Поиск пользователя по имени. */
    public User byUsername(String username) {
        return repo.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found"));
    }
}