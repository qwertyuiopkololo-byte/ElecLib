package com.eleclib.controller;

import com.eleclib.service.AvatarStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AvatarController {
    private final AvatarStorageService avatars;
    @GetMapping("/avatars/{id:\\d+}")
    public ResponseEntity<byte[]> avatar(@PathVariable Long id) {
        byte[] image = avatars.read(id);
        if (image == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(image);
    }
}
