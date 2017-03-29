package io.stardog.stardao.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import io.stardog.stardao.core.Update;

import java.io.IOException;
import java.util.Map;

public class UpdateSerializer extends JsonSerializer<Update<?>> {
    @Override
    public void serialize(Update<?> update, JsonGenerator jsonGen, SerializerProvider serializerProvider) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonGen.getCodec();
        Map<String,Object> partial = mapper.convertValue(update.getPartial(), new TypeReference<Map<String,Object>>() { });

        jsonGen.writeStartObject();
        for (String field : update.getSetFields()) {
            jsonGen.writeObjectField(field, partial.get(field));
        }
        for (String field : update.getRemoveFields()) {
            jsonGen.writeNullField(field);
        }

        jsonGen.writeEndObject();
    }
}
