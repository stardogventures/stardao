package io.stardog.stardao.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.stardog.stardao.core.Update;

/**
 * This helper lets you quickly construct objects for tests, including using single-quotes/unquoted field names
 * to make it easier to construct JSON strings inline Java.
 *
 * It's not intended to be used for production purposes. Just testing.
 */
public class JsonHelper {
    public static ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());

    /**
     * Create an object, given a JSON string and a class.
     * @param json  JSON string
     * @param klazz class of object to create
     * @param <T>   type of object to create
     * @return  object
     */
    public static <T> T object(String json, Class<T> klazz) {
        try {
            return MAPPER.readValue(json, klazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create an Update object, given the class of the update type
     * @param json  JSON string
     * @param klazz class of the update type
     * @param <T>
     * @return
     */
    public static <T> Update<T> update(String json, Class<T> klazz) {
        try {
            JavaType type = MAPPER.getTypeFactory().constructParametricType(Update.class, klazz);
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Turn an object into JSON.
     * @param object    object
     * @return  JSON string of object
     */
    public static String json(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
