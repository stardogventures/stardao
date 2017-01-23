package io.stardog.stardao.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.stardog.stardao.core.Update;

public class JsonHelper {
    private static ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .registerModule(new JavaTimeModule());

    public static <T> T object(String json, Class<T> klazz) {
        try {
            return MAPPER.readValue(json, klazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Update<T> update(String json, Class<T> klazz) {
        try {
            JavaType type = MAPPER.getTypeFactory().constructParametricType(Update.class, klazz);
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
