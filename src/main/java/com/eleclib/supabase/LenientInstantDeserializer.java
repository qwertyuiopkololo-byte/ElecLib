package com.eleclib.supabase;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Deserializes ISO-8601 timestamp strings from Supabase/PostgreSQL that may lack
 * a trailing 'Z' and use variable fractional seconds (e.g. 2026-02-07T20:10:04.910946).
 * Such values are interpreted as UTC.
 */
public class LenientInstantDeserializer extends JsonDeserializer<Instant> {

    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter();

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().trim();
        if (value == null || value.isEmpty()) {
            return null;
        }
        // Standard ISO-8601 with Z is parsed by Instant directly
        if (value.endsWith("Z")) {
            return Instant.parse(value);
        }
        // No zone (e.g. from PostgreSQL): treat as UTC, support 1–9 fractional digits
        LocalDateTime ldt = LocalDateTime.parse(value, FORMATTER);
        return ldt.toInstant(ZoneOffset.UTC);
    }
}
