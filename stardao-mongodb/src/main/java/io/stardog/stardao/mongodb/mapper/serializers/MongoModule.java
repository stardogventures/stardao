package io.stardog.stardao.mongodb.mapper.serializers;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bson.types.Decimal128;

import java.util.Date;

public class MongoModule extends SimpleModule {
    public MongoModule() {
        super();
        addSerializer(Date.class, new DateSerializer());
        addSerializer(Decimal128.class, new Decimal128Serializer());
    }
}
