package com.guerre.backend.service;

import com.guerre.backend.models.Tag;
import com.guerre.backend.repository.InMemoryTagRepository;

import java.util.List;
import java.util.Optional;

public class TagService {

    private final InMemoryTagRepository repo;

    public TagService(InMemoryTagRepository repo) { this.repo = repo; }

    public List<Tag> findAll() { return repo.findAll(); }
    public Optional<Tag> findById(Long id) { return repo.findById(id); }
    public Tag save(Tag t) { return repo.save(t); }
    public void deleteById(Long id) { repo.deleteById(id); }
}
