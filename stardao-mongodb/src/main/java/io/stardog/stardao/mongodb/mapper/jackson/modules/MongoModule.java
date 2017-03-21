package io.stardog.stardao.mongodb.mapper.jackson.modules;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

/**
 * A Jackson module that handles serializing special types that we might get back from MongoDB,
 * for lossless conversion to POJOs.
 */
public class MongoModule extends SimpleModule {
    public MongoModule() {
        super();
        addSerializer(Decimal128.class, new ToStringSerializer());
        addSerializer(ObjectId.class, new ToStringSerializer());
    }
}
