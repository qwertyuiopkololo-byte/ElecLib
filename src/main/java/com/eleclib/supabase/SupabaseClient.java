package com.eleclib.supabase;

import com.eleclib.config.SupabaseConfig.SupabaseProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
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


@Component
@RequiredArgsConstructor
public class SupabaseClient {

    private final RestTemplate restTemplate;
    private final SupabaseProperties supabaseProperties;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new SimpleModule().addDeserializer(Instant.class, new LenientInstantDeserializer()))
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

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
    private Map<String, Object> toMap(Object body) {
        return objectMapper.convertValue(body, new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    /** Тело PATCH/POST без PK и null-полей (иначе PostgREST даёт 409 на duplicate key). */
    @SneakyThrows
    private String jsonForWrite(Object body, String pkColumn) {
        Map<String, Object> map = toMap(body);
        if (pkColumn != null) {
            map.remove(pkColumn);
        }
        map.entrySet().removeIf(e -> e.getValue() == null);
        return objectMapper.writeValueAsString(map);
    }

    @SneakyThrows
    public <T> T post(String table, Object body, Class<T> type) {
        return post(table, null, body, type);
    }

    @SneakyThrows
    public <T> T post(String table, String pkColumn, Object body, Class<T> type) {
        String json = jsonForWrite(body, pkColumn);
        String url = baseUrl() + "/" + table;
        String response = restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(json, headers()), String.class).getBody();
        List<T> list = objectMapper.readValue(response, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Java HttpURLConnection не поддерживает PATCH — PostgREST принимает POST + X-HTTP-Method-Override.
     */
    private void exchangePatch(String url, String json) {
        HttpHeaders h = headers();
        h.set("X-HTTP-Method-Override", "PATCH");
        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(json, h), Void.class);
    }

    @SneakyThrows
    public void patch(String table, String column, Object columnValue, Object body) {
        String json = jsonForWrite(body, column);
        String url = baseUrl() + "/" + table + "?" + column + "=eq." + columnValue;
        exchangePatch(url, json);
    }

    @SneakyThrows
    public void patchComposite(String table, Map<String, Object> keys, Object body) {
        Map<String, Object> map = toMap(body);
        keys.keySet().forEach(map::remove);
        map.entrySet().removeIf(e -> e.getValue() == null);
        String json = objectMapper.writeValueAsString(map);
        String query = keys.entrySet().stream()
                .map(e -> e.getKey() + "=eq." + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
        String url = baseUrl() + "/" + table + "?" + query;
        exchangePatch(url, json);
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
