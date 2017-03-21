package io.stardog.stardao.mongodb.mapper.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bson.types.Decimal128;

import java.io.IOException;

/**
 * A Jackson serializer that renders the BSON Decimal128 type into a String. This is useful for losslessly mapping to
 * BigDecimals.
 */
public class Decimal128Serializer extends JsonSerializer<Decimal128> {
    @Override
    public void serialize(Decimal128 decimal128, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(decimal128.toString());
    }
}

