package com.guerre.backend.service;

import com.guerre.backend.models.Category;
import com.guerre.backend.repository.InMemoryCategoryRepository;

import java.util.List;
import java.util.Optional;

public class CategoryService {

    private final InMemoryCategoryRepository repo;

    public CategoryService(InMemoryCategoryRepository repo) { this.repo = repo; }

    public List<Category> findAll() { return repo.findAll(); }
    public Optional<Category> findById(Long id) { return repo.findById(id); }
    public Category save(Category c) { return repo.save(c); }
    public void deleteById(Long id) { repo.deleteById(id); }
}
