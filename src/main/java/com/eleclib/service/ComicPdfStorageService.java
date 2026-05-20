package com.eleclib.service;

import com.eleclib.model.Book;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class ComicPdfStorageService {

    @Value("${eleclib.comics.dir:data/comics}")
    private String comicsDirProperty;

    private Path comicsDir;

    @PostConstruct
    void init() throws IOException {
        comicsDir = Paths.get(comicsDirProperty).toAbsolutePath().normalize();
        Files.createDirectories(comicsDir);
        log.info("Comic PDF storage: {}", comicsDir);
    }

    public boolean hasPdf(Long bookId) {
        return bookId != null && Files.isRegularFile(pathFor(bookId));
    }

    public Path pathFor(Long bookId) {
        return comicsDir.resolve(bookId + ".pdf");
    }

    public void store(Long bookId, MultipartFile file) throws IOException {
        if (bookId == null || bookId <= 0) {
            throw new IllegalArgumentException("Некорректный id книги");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл PDF не выбран");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        if (!name.endsWith(".pdf") && !contentType.contains("pdf")) {
            throw new IllegalArgumentException("Нужен файл в формате PDF");
        }
        Files.copy(file.getInputStream(), pathFor(bookId), StandardCopyOption.REPLACE_EXISTING);
    }

    public Resource asResource(Long bookId) {
        Path path = pathFor(bookId);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        return new FileSystemResource(path);
    }
}
