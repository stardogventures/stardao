package io.stardog.stardao.mongodb.mapper.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import com.mongodb.MongoException;
import io.stardog.stardao.core.field.Field;
import io.stardog.stardao.core.field.FieldData;
import io.stardog.stardao.mongodb.mapper.DocumentMapper;
import io.stardog.stardao.mongodb.mapper.jackson.modules.ExtendedJsonModule;
import io.stardog.stardao.mongodb.mapper.jackson.modules.MongoModule;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

public class JacksonDocumentMapper<M> implements DocumentMapper<M> {
    private final Class<M> modelClass;
    private final ObjectMapper objectMapper;
    private final ObjectMapper extendedJsonMapper;
    private final Map<String,String> objectToDocumentFieldRenames;
    private final Map<String,String> documentToObjectFieldRenames;

    public final static ObjectMapper DEFAULT_EXTENDED_JSON_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .registerModule(new ExtendedJsonModule());

    public final static ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .registerModule(new MongoModule());

    public JacksonDocumentMapper(Class<M> modelClass) {
        this.modelClass = modelClass;
        this.objectMapper = DEFAULT_OBJECT_MAPPER;
        this.extendedJsonMapper = DEFAULT_EXTENDED_JSON_MAPPER;
        this.objectToDocumentFieldRenames = ImmutableMap.of();
        this.documentToObjectFieldRenames = ImmutableMap.of();
    }

    public JacksonDocumentMapper(Class<M> modelClass, FieldData fieldData) {
        this(modelClass, fieldData, DEFAULT_OBJECT_MAPPER, DEFAULT_EXTENDED_JSON_MAPPER);
    }

    public JacksonDocumentMapper(Class<M> modelClass, FieldData fieldData, ObjectMapper objectMapper, ObjectMapper extendedJsonMapper) {
        this.modelClass = modelClass;
        this.objectMapper = objectMapper;
        this.extendedJsonMapper = extendedJsonMapper;
        this.objectToDocumentFieldRenames = new HashMap<>();
        this.documentToObjectFieldRenames = new HashMap<>();
        for (Field field : fieldData.getMap().values()) {
            if (!field.getStorageName().equals(field.getName())) {
                objectToDocumentFieldRenames.put(field.getName(), field.getStorageName());
                documentToObjectFieldRenames.put(field.getStorageName(), field.getName());
            }
        }
    }

    /**
     * Given a MongoDB Document, convert it to a POJO model, renaming fields as needed, using the Jackson object mapper.
     * @param document  document returned from MongoDB
     * @return  POJO model to convert to
     */
    @Override
    public M toObject(Document document) {
        if (document == null) {
            return null;
        }
        Document renamed = renameDocument(document, documentToObjectFieldRenames);
        return objectMapper.convertValue(renamed, modelClass);
    }

    /**
     * Given a POJO, convert it to a MongoDB Document, renaming fields as needed, by initially transforming to MongoDB
     * extended JSON using Jackson, and then use MongoDB's built-in Document parse().
     * @param object
     * @return
     */
    @Override
    public Document toDocument(M object) {
        if (object == null) {
            return null;
        }
        try {
            String extendedJson = extendedJsonMapper.writeValueAsString(object);
            Document document = Document.parse(extendedJson);
            Document renamed = renameDocument(document, objectToDocumentFieldRenames);
            return renamed;

        } catch (JsonProcessingException e) {
            throw new MongoException("Problem converting object to extended JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Given a document and a set of fields to rename, rename all of the fields.
     * @param doc   document
     * @param renames   map of old field names to new field names
     * @return  document with the field names transformed
     */
    protected Document renameDocument(Document doc, Map<String,String> renames) {
        Document renamedDoc = new Document();
        for (String key : doc.keySet()) {
            String renamedKey = renames.getOrDefault(key, key);
            if (renamedKey != null && !"".equals(renamedKey)) {
                renamedDoc.put(renamedKey, doc.get(key));
            }
        }
        return renamedDoc;
    }
}
