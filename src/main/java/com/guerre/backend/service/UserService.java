package com.guerre.backend.service;

import com.guerre.backend.models.User;
import com.guerre.backend.repository.UserRepositoryPort;

import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserRepositoryPort repo;

    public UserService(UserRepositoryPort repo) { this.repo = repo; }

    public List<User> findAll() { return repo.findAll(); }

    public Optional<User> findById(Long id) { return repo.findById(id); }

    public User save(User user) { return repo.save(user); }

    public void deleteById(Long id) { repo.deleteById(id); }
}
