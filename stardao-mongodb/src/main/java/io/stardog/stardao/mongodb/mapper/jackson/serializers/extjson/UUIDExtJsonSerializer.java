package io.stardog.stardao.mongodb.mapper.jackson.serializers.extjson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.util.Base64;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDExtJsonSerializer extends JsonSerializer<UUID> {
    @Override
    public void serialize(UUID uuid, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        ByteBuffer uuidBytes = ByteBuffer.wrap(new byte[16]);
        uuidBytes.putLong(uuid.getMostSignificantBits());
        uuidBytes.putLong(uuid.getLeastSignificantBits());

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("$binary", Base64.getEncoder().encodeToString(uuidBytes.array()));
        jsonGenerator.writeStringField("$type", "4");
        jsonGenerator.writeEndObject();
    }
}
