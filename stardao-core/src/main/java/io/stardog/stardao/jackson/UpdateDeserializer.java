package io.stardog.stardao.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableSet;
import io.stardog.stardao.core.Update;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UpdateDeserializer extends JsonDeserializer<Update<?>> implements ContextualDeserializer {
    private JavaType valueType;

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctx, BeanProperty property) throws JsonMappingException {
        JavaType wrapperType = (property == null) ? ctx.getContextualType() : property.getType();
        JavaType valueType = wrapperType.containedType(0);

        UpdateDeserializer deserializer = new UpdateDeserializer();
        deserializer.valueType = valueType;
        return deserializer;
    }

    @Override
    public Update<?> deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();

        JsonNode node = mapper.readTree(parser);

        ImmutableSet.Builder<String> setFields = ImmutableSet.builder();
        ImmutableSet.Builder<String> removeFields = ImmutableSet.builder();
        Map<String,JsonNode> setNodes = new HashMap<>();

        Iterator<String> iter = node.fieldNames();
        while (iter.hasNext()) {
            String field = iter.next();
            JsonNode value = node.get(field);
            if (value instanceof NullNode) {
                removeFields.add(field);
            } else if (value instanceof TextNode && value.toString().equals("\"\"")) {
                removeFields.add(field);
            } else {
                setFields.add(field);
                setNodes.put(field, value);
            }
        }
        Object setObject = mapper.convertValue(setNodes, valueType);

        return Update.of(setObject, setFields.build(), removeFields.build());
    }
}
