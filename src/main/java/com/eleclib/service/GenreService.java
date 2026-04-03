package com.eleclib.service;

import com.eleclib.model.Genre;
import com.eleclib.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;

    public List<Genre> findAll() {
        return genreRepository.findAll();
    }

    public Genre findById(Long id) {
        return genreRepository.findById(id).orElse(null);
    }
}
