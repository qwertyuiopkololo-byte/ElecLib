package com.eleclib.service;

import com.eleclib.config.SupabaseConfig.SupabaseProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvatarStorageService {
    private final SupabaseProperties properties;
    private final RestTemplate restTemplate;

    private String base() { return properties.getUrl() + "/storage/v1"; }
    public void verifyBucket() {
        restTemplate.exchange(base() + "/bucket/avatars", HttpMethod.GET, new HttpEntity<>(headers()), String.class);
    }
    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", properties.getKey());
        headers.setBearerAuth(properties.getKey());
        return headers;
    }
    public byte[] read(Long userId) {
        try {
            return restTemplate.exchange(base() + "/object/authenticated/avatars/" + userId + ".png",
                    HttpMethod.GET, new HttpEntity<>(headers()), byte[].class).getBody();
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            if (e.getStatusCode().value() == 404 || body.contains("NoSuchKey") || body.contains("Object not found")) return null;
            throw e;
        }
    }
    public void upload(Long userId, byte[] png) {
        HttpHeaders h = headers(); h.setContentType(MediaType.IMAGE_PNG); h.set("x-upsert", "true");
        restTemplate.exchange(base() + "/object/avatars/" + userId + ".png", HttpMethod.POST,
                new HttpEntity<>(png, h), String.class);
    }
    public void delete(Long userId) {
        HttpHeaders h = headers(); h.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(base() + "/object/avatars", HttpMethod.DELETE,
                new HttpEntity<>(Map.of("prefixes", List.of(userId + ".png")), h), String.class);
    }
}
