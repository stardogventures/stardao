package io.stardog.stardao.mongodb.mapper.jackson.modules;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.stardog.stardao.mongodb.mapper.jackson.serializers.extjson.BigDecimalExtJsonSerializer;
import io.stardog.stardao.mongodb.mapper.jackson.serializers.extjson.InstantExtJsonSerializer;
import io.stardog.stardao.mongodb.mapper.jackson.serializers.extjson.ObjectIdExtJsonSerializer;
import io.stardog.stardao.mongodb.mapper.jackson.serializers.extjson.UUIDExtJsonSerializer;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A Jackson module that can serialize objects into "extended JSON", MongoDB's custom JSON format.
 * From extended JSON, we can transform into a Mongo BSON document.
 */
public class ExtendedJsonModule extends SimpleModule {
    public ExtendedJsonModule() {
        super();
        addSerializer(ObjectId.class, new ObjectIdExtJsonSerializer());
        addSerializer(Instant.class, new InstantExtJsonSerializer());
        addSerializer(BigDecimal.class, new BigDecimalExtJsonSerializer());
        addSerializer(UUID.class, new UUIDExtJsonSerializer());

        // local dates should be stored as strings, not arrays
        addSerializer(LocalDate.class, new ToStringSerializer());
    }
}
