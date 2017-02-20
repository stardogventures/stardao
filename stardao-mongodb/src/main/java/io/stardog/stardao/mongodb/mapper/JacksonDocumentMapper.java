package io.stardog.stardao.mongodb.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import io.stardog.stardao.core.field.Field;
import io.stardog.stardao.core.field.FieldData;
import io.stardog.stardao.mongodb.mapper.serializers.MongoModule;
import org.bson.Document;
import org.mongojack.MongoJsonMappingException;
import org.mongojack.internal.MongoJackModule;
import org.mongojack.internal.object.BsonObjectGenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JacksonDocumentMapper<M> implements DocumentMapper<M> {
    private final Class<M> modelClass;
    private final ObjectMapper objectMapper;
    private final Map<String,String> objectToDocumentFieldRenames;
    private final Map<String,String> documentToObjectFieldRenames;

    public final static ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new MongoJackModule())
            .registerModule(new JavaTimeModule())
            .registerModule(new MongoModule());

    public JacksonDocumentMapper(Class<M> modelClass) {
        this.modelClass = modelClass;
        this.objectMapper = DEFAULT_OBJECT_MAPPER;
        this.objectToDocumentFieldRenames = ImmutableMap.of();
        this.documentToObjectFieldRenames = ImmutableMap.of();
    }

    public JacksonDocumentMapper(Class<M> modelClass, ObjectMapper objectMapper, FieldData fieldData) {
        this.modelClass = modelClass;
        this.objectMapper = objectMapper;
        this.objectToDocumentFieldRenames = new HashMap<>();
        this.documentToObjectFieldRenames = new HashMap<>();
        for (Field field : fieldData.getAll().values()) {
            if (!field.getStorageName().equals(field.getName())) {
                objectToDocumentFieldRenames.put(field.getName(), field.getStorageName());
                documentToObjectFieldRenames.put(field.getStorageName(), field.getName());
            }
        }
    }

    public M toObject(Document document) {
        if (document == null) {
            return null;
        }
        Document renamed = renameDocument(document, documentToObjectFieldRenames);
        return objectMapper.convertValue(renamed, modelClass);
    }

    @Override
    public Document toDocument(M object) {
        if (object == null) {
            return null;
        }
        DBObject dbObject = mongoJackConvert(object);
        Document document = dbObjectToDocument(dbObject);
        Document renamed = renameDocument(document, objectToDocumentFieldRenames);
        return renamed;
    }

    protected Document renameDocument(Document doc, Map<String,String> renames) {
        Document renamedDoc = new Document();
        for (String key : doc.keySet()) {
            String renamedKey = renames.getOrDefault(key, key);
            renamedDoc.put(renamedKey, doc.get(key));
        }
        return renamedDoc;
    }

    public Document dbObjectToDocument(DBObject dbObject) {
        if (dbObject == null) {
            return null;
        }
        Document doc = new Document();
        for (String key : dbObject.keySet()) {
            Object val = dbObject.get(key);
            doc.put(key, val);
        }
        return doc;
    }

    // from mongojack
    private DBObject mongoJackConvert(M object) throws MongoException {
        if (object == null) {
            return null;
        }
        BsonObjectGenerator generator = new BsonObjectGenerator();
        try {
            objectMapper.writeValue(generator, object);
        } catch (JsonMappingException e) {
            throw new MongoJsonMappingException(e);
        } catch (IOException e) {
            // This shouldn't happen
            throw new MongoException(
                    "Unknown error occurred converting BSON to object", e);
        }
        return generator.getDBObject();
    }
}
