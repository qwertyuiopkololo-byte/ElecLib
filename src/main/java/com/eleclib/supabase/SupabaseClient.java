package com.eleclib.supabase;

import com.eleclib.config.SupabaseConfig.SupabaseProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class SupabaseClient {

    private final RestTemplate restTemplate;
    private final SupabaseProperties supabaseProperties;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new SimpleModule().addDeserializer(Instant.class, new LenientInstantDeserializer()))
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @PostConstruct
    void validateSupabaseUrl() {
        String u = supabaseProperties.getUrl();
        if (u == null || u.isBlank() || !(u.startsWith("http://") || u.startsWith("https://"))) {
            throw new IllegalStateException(
                    "supabase.url пустой или неверный. Задайте SUPABASE_URL/SUPABASE_KEY или создайте "
                            + "src/main/resources/application-local.properties по образцу application.properties.example.");
        }
    }

    private String baseUrl() {
        return supabaseProperties.getUrl() + "/rest/v1";
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("apikey", supabaseProperties.getKey());
        h.set("Authorization", "Bearer " + supabaseProperties.getKey());
        h.set("Prefer", "return=representation");
        h.set("Accept-Profile", "public");
        h.set("Content-Profile", "public");
        return h;
    }

    @SneakyThrows
    public <T> List<T> get(String table, Map<String, String> queryParams, Class<T> type) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (queryParams != null) {
            queryParams.forEach((k, v) -> params.add(k, v));
        }
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl() + "/" + table)
                .queryParams(params)
                .build()
                .toUriString();
        String json = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers()), String.class).getBody();
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    public <T> List<T> getAll(String table, Class<T> type) {
        return get(table, null, type);
    }

    public <T> List<T> getByFilter(String table, String column, String value, Class<T> type) {
        return get(table, Map.of(column, "eq." + value), type);
    }

    public <T> List<T> getByFilter(String table, String column, long value, Class<T> type) {
        return getByFilter(table, column, String.valueOf(value), type);
    }

    @SneakyThrows
    public <T> T getOne(String table, String column, Object value, Class<T> type) {
        String val = value instanceof String ? (String) value : String.valueOf(value);
        List<T> list = get(table, Map.of(column, "eq." + val), type);
        return list.isEmpty() ? null : list.get(0);
    }

    public <T> T getById(String table, String idColumn, Long id, Class<T> type) {
        return getOne(table, idColumn, id, type);
    }

    @SneakyThrows
    public <T> T post(String table, Object body, Class<T> type) {
        String json = objectMapper.writeValueAsString(body);
        String url = baseUrl() + "/" + table;
        String response = restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(json, headers()), String.class).getBody();
        List<T> list = objectMapper.readValue(response, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        return list.isEmpty() ? null : list.get(0);
    }

    @SneakyThrows
    public void patch(String table, String column, Object columnValue, Object body) {
        String json = objectMapper.writeValueAsString(body);
        String url = baseUrl() + "/" + table + "?" + column + "=eq." + columnValue;
        restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(json, headers()), Void.class);
    }

    @SneakyThrows
    public void patchComposite(String table, Map<String, Object> keys, Object body) {
        String json = objectMapper.writeValueAsString(body);
        String query = keys.entrySet().stream()
                .map(e -> e.getKey() + "=eq." + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
        String url = baseUrl() + "/" + table + "?" + query;
        restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(json, headers()), Void.class);
    }

    public void delete(String table, String column, Object value) {
        String url = baseUrl() + "/" + table + "?" + column + "=eq." + value;
        restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers()), Void.class);
    }

    public void deleteComposite(String table, Map<String, Object> keys) {
        String query = keys.entrySet().stream()
                .map(e -> e.getKey() + "=eq." + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
        String url = baseUrl() + "/" + table + "?" + query;
        restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers()), Void.class);
    }
}
