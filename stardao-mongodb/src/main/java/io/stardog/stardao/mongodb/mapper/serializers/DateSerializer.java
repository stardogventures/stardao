package io.stardog.stardao.mongodb.mapper.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

/**
 * A Jackson serializer that renders a Date as a UTC ISO-8859-1 string. This is useful for losslessly mapping to better
 * types such as Instant.
 */
public class DateSerializer extends JsonSerializer<Date> {
    @Override
    public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonGenerator.writeString(Instant.ofEpochMilli(date.getTime()).toString());
    }
}
