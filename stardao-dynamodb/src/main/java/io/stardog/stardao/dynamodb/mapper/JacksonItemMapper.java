package io.stardog.stardao.dynamodb.mapper;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JacksonItemMapper<M> implements ItemMapper<M> {
    private final Class<M> modelClass;
    private final ObjectMapper objectMapper;

    public JacksonItemMapper(Class<M> modelClass) {
        this(modelClass, new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
                .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                .registerModule(new JavaTimeModule()));
    }

    public JacksonItemMapper(Class<M> modelClass, ObjectMapper objectMapper) {
        this.modelClass = modelClass;
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Map a DynamoDB Item to a POJO, using the Jackson object mapper.
     * @param item
     * @return
     */
    public M toObject(Item item) {
        if (item == null) {
            return null;
        }
        // not working with millisecond timestamps for some reason:
        //    return OBJECT_MAPPER.convertValue(item.asMap(), toValueType);
        // so do it the slower way:
        Map<String,Object> map = item.asMap();
        try {
            String json = objectMapper.writeValueAsString(map);
            return objectMapper.readValue(json, modelClass);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Map a POJO to a DynamoDB Item, using the Jackson object mapper.
     * @param object
     * @return
     */
    public Item toItem(Object object) {
        try {
            String json = objectMapper.writeValueAsString(object);
            Item item = Item.fromJSON(json);

            // remove any empty strings, since DynamoDB cannot store them
            List<String> removeAttrs = new ArrayList<>();
            for (Map.Entry<String,Object> attr : item.attributes()) {
                if ("".equals(attr.getValue())) {
                    removeAttrs.add(attr.getKey());
                }
            }
            for (String attrName : removeAttrs) {
                item.removeAttribute(attrName);
            }

            return item;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to convert " + object);
        }
    }
}