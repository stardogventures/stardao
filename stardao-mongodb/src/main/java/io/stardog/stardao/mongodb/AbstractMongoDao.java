package io.stardog.stardao.mongodb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexModel;
import io.stardog.stardao.core.AbstractDao;
import io.stardog.stardao.core.Results;
import io.stardog.stardao.core.Update;
import io.stardog.stardao.core.field.Field;
import io.stardog.stardao.core.field.FieldData;
import io.stardog.stardao.exceptions.DataNotFoundException;
import io.stardog.stardao.mongodb.mapper.DocumentMapper;
import io.stardog.stardao.mongodb.mapper.jackson.JacksonDocumentMapper;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public abstract class AbstractMongoDao<M,P,K,I> extends AbstractDao<M,P,K,I> {
    private final MongoCollection<Document> collection;
    private final DocumentMapper<M> modelMapper;
    private final DocumentMapper<P> partialMapper;
    public static final String ID_FIELD = "_id";

    public AbstractMongoDao(Class<M> modelClass, Class<P> partialClass, MongoCollection<Document> collection) {
        super(modelClass, partialClass);
        this.collection = collection;
        this.modelMapper = new JacksonDocumentMapper<>(modelClass, getFieldData());
        this.partialMapper = new JacksonDocumentMapper<>(partialClass, getFieldData());
    }

    public AbstractMongoDao(Class<M> modelClass, Class<P> partialClass, MongoCollection<Document> collection, ObjectMapper objectMapper, ObjectMapper extendedJsonMapper) {
        super(modelClass, partialClass);
        this.collection = collection;
        this.modelMapper = new JacksonDocumentMapper<>(modelClass, getFieldData(), objectMapper, extendedJsonMapper);
        this.partialMapper = new JacksonDocumentMapper<>(partialClass, getFieldData(), objectMapper, extendedJsonMapper);
    }

    public AbstractMongoDao(Class<M> modelClass, Class<P> partialClass, MongoCollection<Document> collection,
                            DocumentMapper<M> modelMapper, DocumentMapper<P> partialMapper) {
        super(modelClass, partialClass);
        this.collection = collection;
        this.modelMapper = modelMapper;
        this.partialMapper = partialMapper;
    }

    /**
     * MongoDB always stores the id field as _id, so any id field should automatically get assigned @StoragePath("_id")
     * @return FieldData
     */
    @Override
    protected FieldData generateFieldData() {
        FieldData fieldData = super.generateFieldData();
        Field id = fieldData.getId();
        if (id != null) {
            id = id.toBuilder().storageName(ID_FIELD).build();
            Map<String,Field> fieldMap = new HashMap<>();
            fieldMap.putAll(fieldData.getMap());
            fieldMap.put(id.getName(), id);
            fieldData = fieldData.toBuilder().id(id).map(fieldMap).build();
        }
        return fieldData;
    }

    public MongoCollection<Document> getCollection() {
        return collection;
    }

    public String getCollectionName() {
        return getCollection().getNamespace().getCollectionName();
    }

    public DocumentMapper<M> getModelMapper() {
        return modelMapper;
    }

    public DocumentMapper<P> getPartialMapper() {
        return partialMapper;
    }

    protected Object generateId() {
        return new ObjectId();
    }

    /**
     * Load an object that might not exist by id. Will return empty Optional if the document is not present.
     * @param id    primary key value
     * @return  optional containing the object, or empty if not found
     */
    @Override
    public Optional<M> loadOpt(K id) {
        Document query = new Document(ID_FIELD, id);
        Document doc = getCollection().find(query).limit(1).first();
        return Optional.ofNullable(modelMapper.toObject(doc));
    }

    /**
     * Load a partial object that might not exist by id.
     * @param id    primary key value
     * @param fields    set of fields to return
     * @return
     */
    public Optional<P> loadOpt(K id, Iterable<String> fields) {
        Document query = new Document(ID_FIELD, id);
        Document project = new Document();
        FieldData fieldData = getFieldData();
        for (String fieldName : fields) {
            Field internalField = fieldData.getMap().get(fieldName);
            if (internalField == null) {
                throw new IllegalArgumentException("Unknown field: " + fieldName);
            }
            String storageName = internalField.getStorageName();
            project.append(storageName, 1);
        }
        Document doc = getCollection().find(query).projection(project).limit(1).first();
        return Optional.ofNullable(partialMapper.toObject(doc));
    }

    /**
     * Load a single object by an arbitrary MongoDB query. Will return the first match of the query.
     * @param query MongoDB query
     * @return  model object
     * @throws DataNotFoundException    if there is no object matching the query
     */
    protected M loadByQuery(Bson query) {
        return loadByQuery(query, null);
    }

    /**
     * Load a single object by an arbitrary MongoDB query, applying a sort. Will return the first match of the query.
     * @param query MongoDB query
     * @param sort  MongoDB sort object
     * @return  model object
     * @throws DataNotFoundException    if there is no object matching the query
     */
    protected M loadByQuery(Bson query, Bson sort) {
        FindIterable<Document> find = getCollection().find(query);
        if (sort != null) {
            find.sort(sort);
        }
        Document doc = find.limit(1).first();
        if (doc == null) {
            throw new DataNotFoundException(getDisplayModelName() + " not found");
        }
        return modelMapper.toObject(doc);
    }

    /**
     * Load a single object by an arbitrary MongoDB query. Will return an Optional containing the first match found, or
     * empty Optional if not found.
     * @param query MongoDB query
     * @return  Optional containing the found object
     */
    protected Optional<M> loadByQueryOpt(Bson query) {
        return loadByQueryOpt(query, null);
    }

    /**
     * Load a single object by an arbitrary MongoDB query and sort order. Will return an Optional containing the first
     * match found, or empty Optional if not found.
     * @param query MongoDB query
     * @param sort  MongoDB sort object
     * @return  Optional containing the found object
     */
    protected Optional<M> loadByQueryOpt(Bson query, Bson sort) {
        FindIterable<Document> find = getCollection().find(query);
        if (sort != null) {
            find.sort(sort);
        }
        Document doc = find.limit(1).first();
        if (doc == null) {
            return Optional.empty();
        }
        return Optional.of(modelMapper.toObject(doc));
    }

    /**
     * Given a query and a sort, return all documents that match the query in sorted order.
     * @param query MongoDB query
     * @param sort  MongoDB sort order
     * @return  results of model objects
     */
    protected Results<M,K> findByQuery(Bson query, Bson sort) {
        return Results.of(iterateByQuery(query, sort));
    }

    /**
     * Given a query and a sort, iterate through all documents that match the query in sorted order.
     * @param query MongoDB query
     * @param sort  MongoDB sort order
     * @return  iterable of model objects
     */
    protected Iterable<M> iterateByQuery(Bson query, Bson sort) {
        FindIterable<Document> iterable = getCollection().find(query);
        if (sort != null) {
            iterable = iterable.sort(sort);
        }
        return iterable.map(doc -> getModelMapper().toObject(doc));
    }

    /**
     * Given a query, a sort, and a projection of fields to return, return all documents as partials
     * that match the query in sorted order
     * @param query MongoDB query
     * @param sort  MongoDB sort order
     * @param projection    MongoDB projection
     * @return  results of partial objects
     */
    protected Results<P,K> findByQuery(Bson query, Bson sort, Bson projection) {
        return Results.of(iterateByQuery(query, sort, projection));
    }

    /**
     * Given a query, a sort, and a projection of fields to return, iterate through all documents as partials
     * that match the query in sorted order
     * @param query MongoDB query
     * @param sort  MongoDB sort order
     * @param projection    MongoDB projection
     * @return  iterable of partial objects
     */
    protected Iterable<P> iterateByQuery(Bson query, Bson sort, Bson projection) {
        FindIterable<Document> iterable = getCollection().find(query);
        if (sort != null) {
            iterable = iterable.sort(sort);
        }
        if (projection != null) {
            iterable = iterable.projection(projection);
        }
        return iterable.map(doc -> getPartialMapper().toObject(doc));
    }

    @Override
    public Iterable<M> iterateAll() {
        return getCollection().find().map((d) -> modelMapper.toObject(d));
    }

    /**
     * Given a FindIterable query, add skip and limit to the query and return a resultset. The results will have
     * a next integer as the next "skip" value to use, or empty optional if we've exhausted results.
     * @param iterable  query
     * @param skip  number of results to skip
     * @param limit number of results to limit
     * @return  results containing up to limit objects found in the query
     */
    protected Results<M, Integer> findWithSkipLimitPagination(FindIterable<Document> iterable, int skip, int limit) {
        ImmutableList.Builder<M> builder = ImmutableList.builder();
        M mostRecentObject = null;

        // query for one more object than we actually need, in order to determine whether there is a "next" page
        int foundCount = 0;
        for (Document doc : iterable.skip(skip).limit(limit+1)) {
            if (mostRecentObject != null) {
                builder.add(mostRecentObject);
            }
            mostRecentObject = modelMapper.toObject(doc);
            foundCount++;
        }

        if (foundCount <= 0) {
            return Results.of(builder.build());
        } else if (foundCount <= limit) {
            builder.add(mostRecentObject);
            return Results.of(builder.build());
        } else {
            return Results.of(builder.build(), skip+limit);
        }
    }

    /**
     * Given a FindIterable query, paginate by using a field value as the "next". The iterable should already contain
     * a query that performs the appropriate comparison on the next (normally a $gte comparison)
     *
     * Because it avoids the use of the inefficient "skip", this type of pagination will be much faster and should be
     * preferred whenever traversing an index.
     *
     * For example:
     *    Document query = new Document("email", new Document("$gte", "bob@example.com"));
     *    Document sort = new Document("email", 1);
     *    findWithRangedPagination(getCollection.find(query).sort(sort), "email", String.class, 20)
     * Will return the next 20 results starting with "bob@example.com"
     *
     * @param iterable  query
     * @param nextField the name of the field to extract as the "next" item
     * @param nextFieldType the class of the expected value of the "next" field (*as it is stored in MongoDB*)
     * @param limit number of results to limit
     * @return  results containing up to limit results in the query, and the value of the "next" field
     */
    protected <N> Results<M, N> findWithRangedPagination(FindIterable<Document> iterable, String nextField, Class<N> nextFieldType, int limit) {
        ImmutableList.Builder<M> builder = ImmutableList.builder();
        M mostRecentObject = null;
        N mostRecentNext = null;

        // query for one more object than we actually need, in order to determine whether there is a "next" page
        int foundCount = 0;
        for (Document doc : iterable.limit(limit + 1)) {
            if (mostRecentObject != null) {
                builder.add(mostRecentObject);
            }
            mostRecentNext = getFieldValue(doc, nextField, nextFieldType);
            mostRecentObject = modelMapper.toObject(doc);
            foundCount++;
        }

        if (foundCount <= 0) {
            return Results.of(builder.build());
        } else if (foundCount <= limit) {
            builder.add(mostRecentObject);
            return Results.of(builder.build());
        } else {
            return Results.of(builder.build(), mostRecentNext);
        }
    }

    protected <T> T getFieldValue(Document doc, String field, Class<T> type) {
        if (type == LocalDate.class) {
            return type.cast(LocalDate.parse(doc.getString(field)));
        } else if (type == Instant.class) {
            Date value = doc.get(field, Date.class);
            return value != null ? type.cast(value.toInstant()) : null;
        } else {
            return doc.get(field, type);
        }
    }

    /**
     * Given a query, check if there are any documents matching that query, with the exception of excludeId.
     * Especially useful for checking whether fields are unique.
     * @param query MongoDB query
     * @param excludeId key value to exclude from consideration
     * @return  true if
     */
    protected boolean exists(Document query, K excludeId) {
        FindIterable<Document> find = getCollection().find(query).projection(new Document("_id", 1)).limit(2);
        for (Document doc : find) {
            if (!doc.get("_id").equals(excludeId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public M create(P partial, Instant createAt, I createBy) {
        Document doc = partialMapper.toDocument(partial);
        if (doc.get(ID_FIELD) == null) {
            doc.put(ID_FIELD, generateId());
        }
        FieldData fieldData = getFieldData();
        if (createAt != null && fieldData.getCreatedAt() != null
                && doc.get(fieldData.getCreatedAt().getStorageName()) == null) {
            doc.put(fieldData.getCreatedAt().getStorageName(), Date.from(createAt));
        }
        if (createBy != null && fieldData.getCreatedBy() != null
                && doc.get(fieldData.getCreatedBy().getStorageName()) == null) {
            doc.put(fieldData.getCreatedBy().getStorageName(), createBy);
        }
        // automatically pre-populate updatedAt/updatedBy, if those fields are non-optional
        if (createAt != null && fieldData.getUpdatedAt() != null
                && !fieldData.getUpdatedAt().isOptional()
                && doc.get(fieldData.getUpdatedAt().getStorageName()) == null) {
            doc.put(fieldData.getUpdatedAt().getStorageName(), Date.from(createAt));
        }
        if (createBy != null && fieldData.getUpdatedBy() != null
                && !fieldData.getUpdatedBy().isOptional()
                && doc.get(fieldData.getUpdatedBy().getStorageName()) == null) {
            doc.put(fieldData.getUpdatedBy().getStorageName(), createBy);
        }
        M model = modelMapper.toObject(doc);
        getCollection().insertOne(doc);
        return model;
    }

    @Override
    public void update(K id, Update<P> update, Instant updateAt, I updateBy) {
        Document query = new Document(ID_FIELD, id);
        Document upDoc = toUpdateDocument(update, updateAt, updateBy);
        getCollection().updateOne(query, upDoc);
    }

    @Override
    public M updateAndReturn(K id, Update<P> update, Instant updateAt, I updateBy) {
        Document upDoc = toUpdateDocument(update, updateAt, updateBy);
        Document query = new Document(ID_FIELD, id);
        Document found = getCollection().findOneAndUpdate(query, upDoc);
        return modelMapper.toObject(found);
    }

    protected Document toUpdateDocument(Update<P> update, Instant updateAt, I updateBy) {
        Document doc = new Document();

        Document setFields = partialMapper.toDocument(update.getPartial());
        Document set = new Document();
        for (String field : update.getSetFields()) {
            set.put(field, setFields.get(field));
        }

        FieldData fieldData = getFieldData();
        if (updateAt != null && fieldData.getUpdatedAt() != null) {
            set.put(fieldData.getUpdatedAt().getStorageName(), Date.from(updateAt));
        }
        if (updateBy != null && fieldData.getUpdatedBy() != null) {
            set.put(fieldData.getUpdatedBy().getStorageName(), updateBy);
        }

        Document unset = new Document();
        for (String field : update.getRemoveFields()) {
            unset.put(field, 1);
        }

        if (!set.isEmpty()) {
            doc.put("$set", set);
        }

        if (!unset.isEmpty()) {
            doc.put("$unset", unset);
        }

        return doc;
    }

    @Override
    public void delete(K id) {
        Document query = new Document(ID_FIELD, id);
        getCollection().deleteOne(query);
    }

    @Override
    public void initTable() {
        for (IndexModel index : getIndexes()) {
            getCollection().createIndex(index.getKeys(), index.getOptions());
        }
    }

    @Override
    public void dropTable() {
        getCollection().drop();
    }

    public List<IndexModel> getIndexes() {
        return ImmutableList.of();
    }

    public Update<P> updateOf(P object) {
        ImmutableSet.Builder<String> attribs = ImmutableSet.builder();
        if (object != null) {
            Document doc = partialMapper.toDocument(object);
            for (String key : doc.keySet()) {
                attribs.add(key);
            }
        }
        return Update.of(object, attribs.build());
    }

    public Update<P> updateOf(P object, Iterable<String> removeFields) {
        ImmutableSet.Builder<String> attribs = ImmutableSet.builder();
        if (object != null) {
            Document doc = partialMapper.toDocument(object);
            for (String key : doc.keySet()) {
                attribs.add(key);
            }
        }
        return Update.of(object, attribs.build(), ImmutableSet.copyOf(removeFields));
    }
}
