package io.stardog.stardao.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import io.stardog.stardao.mapper.serializers.MongoModule;
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

    public JacksonDocumentMapper(Class<M> modelClass, ObjectMapper objectMapper, Map<String, String> objectToDocumentFieldRenames) {
        this.modelClass = modelClass;
        this.objectMapper = objectMapper;
        this.objectToDocumentFieldRenames = objectToDocumentFieldRenames;
        this.documentToObjectFieldRenames = new HashMap<>();
        for (Map.Entry<String,String> entry : objectToDocumentFieldRenames.entrySet()) {
            documentToObjectFieldRenames.put(entry.getValue(), entry.getKey());
        }
    }

    public M toObject(Document document) {
        if (document == null) {
            return null;
        }
        Document renamedDocument = new Document();
        for (String key : document.keySet()) {
            String renamedKey = documentToObjectFieldRenames.getOrDefault(key, key);
            renamedDocument.put(renamedKey, document.get(key));
        }
        return objectMapper.convertValue(renamedDocument, modelClass);
    }

    @Override
    public Document toDocument(M object) {
        if (object == null) {
            return null;
        }
        DBObject dbObject = mongoJackConvert(object);
        Document document = dbObjectToDocument(dbObject);
        for (String key : objectToDocumentFieldRenames.keySet()) {
            if (document.containsKey(key)) {
                String renamedKey = objectToDocumentFieldRenames.get(key);
                document.put(renamedKey, document.get(key));
                document.remove(key);
            }
        }
        return document;
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
