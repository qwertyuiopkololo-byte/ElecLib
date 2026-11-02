package com.eleclib.controller;

import com.eleclib.model.User;
import com.eleclib.service.*;
import com.eleclib.supabase.SupabaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.ui.ExtendedModelMap;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileControllerTest {
    UserService users;
    SupabaseClient supabase;
    AvatarStorageService avatars;
    ProfileController controller;
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    RedirectAttributesModelMap flash;
    @BeforeEach void setUp() {
        users = mock(UserService.class); supabase = mock(SupabaseClient.class); avatars = mock(AvatarStorageService.class);
        controller = new ProfileController(users, mock(SubscriptionService.class), supabase, encoder, avatars);
        when(users.getCurrentUser()).thenReturn(User.builder().userId(7L).login("reader")
                .password(encoder.encode("old-password")).build());
        flash = new RedirectAttributesModelMap();
    }
    @Test void guestIsRedirectedAndCannotWriteProfile() {
        when(users.getCurrentUser()).thenReturn(null);
        assertEquals("redirect:/login", controller.details("Name", "Surname", flash));
        assertEquals("redirect:/login", controller.password("old-password", "new-password", "new-password", flash));
        assertEquals("redirect:/login", controller.profile(new ExtendedModelMap()));
        verifyNoInteractions(supabase, avatars);
    }
    @Test void detailsAreTrimmedAndSavedForCurrentUser() {
        assertEquals("redirect:/profile", controller.details("  Ivan ", " Petrov  ", flash));
        verify(supabase).patch("users", "user_id", 7L, Map.of("first_name", "Ivan", "last_name", "Petrov"));
        assertTrue(flash.getFlashAttributes().containsKey("success"));
    }
    @ParameterizedTest @ValueSource(strings = {"", "   "})
    void blankNameIsRejected(String value) {
        controller.details(value, "Surname", flash);
        verifyNoInteractions(supabase); assertTrue(flash.getFlashAttributes().containsKey("error"));
    }
    @Test void oversizedNameIsRejected() {
        controller.details("a".repeat(101), "Surname", flash);
        verifyNoInteractions(supabase);
    }
    @Test void wrongOldPasswordIsRejected() {
        controller.password("wrong", "new-password", "new-password", flash);
        verifyNoInteractions(supabase); assertTrue(flash.getFlashAttributes().containsKey("error"));
    }
    @ParameterizedTest @ValueSource(strings = {"short", "1234567"})
    void shortNewPasswordIsRejected(String password) {
        controller.password("old-password", password, password, flash);
        verifyNoInteractions(supabase); assertTrue(flash.getFlashAttributes().containsKey("error"));
    }
    @Test void bcryptByteLimitIsEnforcedForUnicode() {
        controller.password("old-password", "я".repeat(37), "я".repeat(37), flash);
        verifyNoInteractions(supabase);
    }
    @Test void mismatchedConfirmationIsRejected() {
        controller.password("old-password", "new-password", "different", flash);
        verifyNoInteractions(supabase);
    }
    @Test void unchangedPasswordIsRejected() {
        controller.password("old-password", "old-password", "old-password", flash);
        verifyNoInteractions(supabase);
    }
    @Test void acceptedPasswordIsStoredAsBcryptHash() {
        controller.password("old-password", "new-password", "new-password", flash);
        verify(supabase).patch(eq("users"), eq("user_id"), eq(7L), argThat(body -> {
            String hash = (String) ((Map<?, ?>) body).get("password");
            return !hash.equals("new-password") && encoder.matches("new-password", hash);
        }));
        assertTrue(flash.getFlashAttributes().containsKey("success"));
    }
    @Test void failedProfileWriteDoesNotShowSuccess() {
        doThrow(new IllegalStateException("offline")).when(supabase).patch(anyString(), anyString(), any(), any());
        controller.details("Ivan", "Petrov", flash);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
        assertFalse(flash.getFlashAttributes().containsKey("success"));
    }
    @Test void validAvatarIsCroppedAndStoredAs256PixelPng() throws Exception {
        BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setColor(java.awt.Color.RED); g.fillRect(0, 0, 300, 200);
        g.setColor(java.awt.Color.BLUE); g.fillRect(50, 0, 200, 200); g.dispose();
        var bytes = new ByteArrayOutputStream(); ImageIO.write(image, "jpeg", bytes);
        controller.uploadAvatar(new MockMultipartFile("avatar", "photo.jpg", "image/jpeg", bytes.toByteArray()), flash);
        var captor = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(avatars).upload(eq(7L), captor.capture());
        var result = ImageIO.read(new ByteArrayInputStream(captor.getValue()));
        assertEquals(256, result.getWidth()); assertEquals(256, result.getHeight());
        var center = new java.awt.Color(result.getRGB(128, 128));
        assertTrue(center.getBlue() > 240 && center.getRed() < 10 && center.getGreen() < 10,
                "The central blue square must be preserved; JPEG may change RGB values slightly");
        assertTrue(flash.getFlashAttributes().containsKey("success"));
    }
    @Test void fakeImageIsRejectedDespiteMimeType() {
        controller.uploadAvatar(new MockMultipartFile("avatar", "fake.png", "image/png", "not an image".getBytes()), flash);
        verifyNoInteractions(avatars); assertTrue(flash.getFlashAttributes().containsKey("error"));
    }
    @Test void oversizedAvatarIsRejected() {
        controller.uploadAvatar(new MockMultipartFile("avatar", "large.png", "image/png", new byte[1_000_001]), flash);
        verifyNoInteractions(avatars);
    }
    @Test void emptyAvatarIsRejected() {
        controller.uploadAvatar(new MockMultipartFile("avatar", new byte[0]), flash);
        verifyNoInteractions(avatars);
    }
    @Test void avatarDeletionUsesCurrentUser() {
        controller.deleteAvatar(flash);
        verify(avatars).delete(7L);
        assertTrue(flash.getFlashAttributes().containsKey("success"));
    }
}
