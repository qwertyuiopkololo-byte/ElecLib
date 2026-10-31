package com.eleclib.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.file.*;

@RestController
public class AvatarController {
    @GetMapping("/avatars/{id:\\d+}")
    public ResponseEntity<byte[]> avatar(@PathVariable Long id) throws java.io.IOException {
        Path path = Path.of("data", "avatars", id + ".png");
        if (!Files.isRegularFile(path)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .body(Files.readAllBytes(path));
    }
}
