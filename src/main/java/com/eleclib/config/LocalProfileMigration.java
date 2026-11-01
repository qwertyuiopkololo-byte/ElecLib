package com.eleclib.config;

import com.eleclib.service.AvatarStorageService;
import com.eleclib.service.MonthlyGoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.time.*;
import java.util.Properties;

/** Explicit one-time import; local backups are never deleted. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "eleclib.migration.local-profile", havingValue = "true")
public class LocalProfileMigration implements ApplicationRunner {
    private final AvatarStorageService avatars;
    private final MonthlyGoalService goals;
    private final com.eleclib.repository.UserRepository users;
    private final com.eleclib.repository.BookRepository books;
    private final com.eleclib.supabase.SupabaseClient supabase;

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        avatars.verifyBucket();
        log.info("Supabase profile storage verified: goals={}, completions={}, avatars bucket available",
                supabase.count("monthly_reading_goals", java.util.Map.of()),
                supabase.count("monthly_reading_completions", java.util.Map.of()));
        Path avatarDirectory = Path.of("data", "avatars");
        if (Files.isDirectory(avatarDirectory)) {
            try (var files = Files.newDirectoryStream(avatarDirectory, "*.png")) {
                for (Path file : files) {
                    String name = file.getFileName().toString();
                    if (!name.matches("\\d+\\.png")) continue;
                    Long id = Long.valueOf(name.replace(".png", ""));
                    if (users.findById(id).isEmpty()) { log.warn("Avatar import skipped: user {} does not exist", id); continue; }
                    if (avatars.read(id) == null) avatars.upload(id, Files.readAllBytes(file));
                }
            }
        }
        Path goalDirectory = Path.of("data", "reading-goals");
        if (Files.isDirectory(goalDirectory)) {
            try (var files = Files.newDirectoryStream(goalDirectory, "*.properties")) {
                for (Path file : files) {
                    String name = file.getFileName().toString();
                    if (!name.matches("\\d+\\.properties")) continue;
                    Long id = Long.valueOf(name.replace(".properties", ""));
                    if (users.findById(id).isEmpty()) { log.warn("Goal import skipped: user {} does not exist; local backup retained", id); continue; }
                    Properties data = new Properties();
                    try (var input = Files.newInputStream(file)) { data.load(input); }
                    for (String key : data.stringPropertyNames()) {
                        if (key.matches("target\\.\\d{4}-\\d{2}")) {
                            goals.saveTarget(id, YearMonth.parse(key.substring(7)), Integer.parseInt(data.getProperty(key)), true);
                        } else if (key.matches("finished\\.\\d{4}-\\d{2}\\.\\d+")) {
                            String[] parts = key.split("\\.");
                            if (books.findById(Long.valueOf(parts[2])).isEmpty()) { log.warn("Completion import skipped: book {} does not exist", parts[2]); continue; }
                            goals.saveFinished(id, Long.valueOf(parts[2]), YearMonth.parse(parts[1]), LocalDate.parse(data.getProperty(key)));
                        }
                    }
                }
            }
        }
        log.info("Local profile data imported into Supabase. Disable eleclib.migration.local-profile on next launch.");
    }
}
