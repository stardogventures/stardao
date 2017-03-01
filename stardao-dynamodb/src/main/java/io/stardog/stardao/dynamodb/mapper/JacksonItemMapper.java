package io.stardog.stardao.dynamodb.mapper;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.stardog.stardao.core.field.Field;
import io.stardog.stardao.core.field.FieldData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JacksonItemMapper<M> implements ItemMapper<M> {
    private final Class<M> modelClass;
    private final FieldData fieldData;
    private final ObjectMapper objectMapper;
    private final Map<String,String> objectToItemFieldRenames;
    private final Map<String,String> itemToObjectFieldRenames;

    public JacksonItemMapper(Class<M> modelClass, FieldData fieldData) {
        this(modelClass, fieldData, new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
                .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule()));
    }

    public JacksonItemMapper(Class<M> modelClass, FieldData fieldData, ObjectMapper objectMapper) {
        this.modelClass = modelClass;
        this.fieldData = fieldData;
        this.objectMapper = objectMapper;
        this.objectToItemFieldRenames = new HashMap<>();
        this.itemToObjectFieldRenames = new HashMap<>();
        for (Field field : fieldData.getMap().values()) {
            if (!field.getStorageName().equals(field.getName())) {
                objectToItemFieldRenames.put(field.getName(), field.getStorageName());
                itemToObjectFieldRenames.put(field.getStorageName(), field.getName());
            }
        }
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Map a DynamoDB Item to a POJO, using the Jackson object modelMapper.
     * @param item
     * @return
     */
    public M toObject(Item item) {
        if (item == null) {
            return null;
        }
        item = renameItem(item, itemToObjectFieldRenames);
        Map<String,Object> map = item.asMap();
        try {
            String json = objectMapper.writeValueAsString(map);
            return objectMapper.readValue(json, modelClass);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Map a POJO to a DynamoDB Item, using the Jackson object modelMapper.
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

            item = renameItem(item, objectToItemFieldRenames);
            return item;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to convert " + object, e);
        }
    }

    protected Item renameItem(Item item, Map<String,String> renames) {
        Map<String,Object> map = item.asMap();
        Item renamedItem = new Item();
        for (String key : map.keySet()) {
            String renamedKey = renames.getOrDefault(key, key);
            renamedItem.with(renamedKey, item.get(key));
        }
        return renamedItem;
    }
}
