package io.stardog.stardao.mongodb.mapper.serializers;

import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.Date;

public class MongoModule extends SimpleModule {
    public MongoModule() {
        super();
        addSerializer(Date.class, new DateSerializer());
    }
}
