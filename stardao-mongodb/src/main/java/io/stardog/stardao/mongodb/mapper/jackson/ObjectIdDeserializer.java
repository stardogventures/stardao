package io.stardog.stardao.mongodb.mapper.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.bson.types.ObjectId;

import java.io.IOException;

public class ObjectIdDeserializer extends JsonDeserializer<ObjectId> {
    @Override
    public ObjectId deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
        String id = parser.getValueAsString();
        if ("".equals(id)) {
            return null;
        }
        return new ObjectId(id);
    }
}
