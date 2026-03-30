package com.guerre.backend.service;

import com.guerre.backend.models.User;
import com.guerre.backend.repository.UserRepositoryPort;

import java.util.Optional;

public class AuthService {

    private final UserRepositoryPort userRepository;

    public AuthService(UserRepositoryPort userRepository) { this.userRepository = userRepository; }

    public Optional<User> authenticate(String email, String motDePasse) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getMotDePasse() != null && u.getMotDePasse().equals(motDePasse));
    }
}
