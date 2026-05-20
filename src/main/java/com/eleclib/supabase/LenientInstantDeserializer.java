package com.eleclib.supabase;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

public class LenientInstantDeserializer extends JsonDeserializer<Instant> {

    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter();

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String raw = p.getText();
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // Fall through to additional tolerant parsing below.
        }

        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            // Fall through to legacy local datetime format.
        }

        LocalDateTime ldt = LocalDateTime.parse(value, FORMATTER);
        return ldt.toInstant(ZoneOffset.UTC);
    }
}
