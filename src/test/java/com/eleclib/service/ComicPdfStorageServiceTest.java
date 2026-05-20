package com.eleclib.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ComicPdfStorageServiceTest {

    @TempDir
    Path tempDir;

    private ComicPdfStorageService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ComicPdfStorageService();
        ReflectionTestUtils.setField(service, "comicsDirProperty", tempDir.toString());
        service.init();
    }

    @Test
    void hasPdf_falseWhenFileMissing() {
        assertFalse(service.hasPdf(42L));
    }

    @Test
    void hasPdf_trueAfterStore() throws Exception {
        byte[] pdfHeader = "%PDF-1.4\n".getBytes();
        MockMultipartFile file = new MockMultipartFile("pdfFile", "comic.pdf", "application/pdf", pdfHeader);
        service.store(7L, file);
        assertTrue(service.hasPdf(7L));
        assertTrue(Files.exists(service.pathFor(7L)));
    }

    @Test
    void asResource_nullWhenMissing() {
        assertNull(service.asResource(99L));
    }

    @Test
    void asResource_readableAfterStore() throws Exception {
        MockMultipartFile file = new MockMultipartFile("pdfFile", "x.pdf", "application/pdf", "%PDF-1.4".getBytes());
        service.store(3L, file);
        Resource resource = service.asResource(3L);
        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void store_rejectsNonPdf() {
        MockMultipartFile file = new MockMultipartFile("pdfFile", "notes.txt", "text/plain", "hello".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.store(1L, file));
    }
}
