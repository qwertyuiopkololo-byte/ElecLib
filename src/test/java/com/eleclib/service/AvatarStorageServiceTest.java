package com.eleclib.service;

import com.eleclib.config.SupabaseConfig.SupabaseProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AvatarStorageServiceTest {
    MockRestServiceServer server;
    AvatarStorageService service;
    @BeforeEach void setUp() {
        var template = new RestTemplate(); server = MockRestServiceServer.bindTo(template).build();
        var properties = new SupabaseProperties();
        properties.setUrl("https://storage.example"); properties.setKey("test-key-not-a-real-secret");
        service = new AvatarStorageService(properties, template);
    }
    @Test void uploadUsesPrivateBucketAndUpsert() {
        byte[] png = {1, 2, 3};
        server.expect(requestTo("https://storage.example/storage/v1/object/avatars/7.png"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer test-key-not-a-real-secret"))
                .andExpect(header("x-upsert", "true")).andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        service.upload(7L, png); server.verify();
    }
    @Test void missingAvatarReturnsNull() {
        server.expect(requestTo("https://storage.example/storage/v1/object/authenticated/avatars/7.png"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertNull(service.read(7L)); server.verify();
    }
    @Test void authenticationFailureIsNotHiddenAsMissingAvatar() {
        server.expect(requestTo("https://storage.example/storage/v1/object/authenticated/avatars/7.png"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThrows(HttpClientErrorException.class, () -> service.read(7L)); server.verify();
    }
    @Test void deleteTargetsOnlyCurrentUsersObject() {
        server.expect(requestTo("https://storage.example/storage/v1/object/avatars"))
                .andExpect(method(HttpMethod.DELETE)).andExpect(content().json("{\"prefixes\":[\"7.png\"]}"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        service.delete(7L); server.verify();
    }
}
