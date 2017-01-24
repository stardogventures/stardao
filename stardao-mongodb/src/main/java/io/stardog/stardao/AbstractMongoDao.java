package io.stardog.stardao;

import com.google.common.collect.ImmutableList;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexModel;
import io.stardog.stardao.core.AbstractDao;
import io.stardog.stardao.core.Results;
import io.stardog.stardao.core.Update;
import io.stardog.stardao.core.field.Field;
import io.stardog.stardao.core.field.FieldData;
import io.stardog.stardao.exceptions.DataNotFoundException;
import io.stardog.stardao.mapper.DocumentMapper;
import io.stardog.stardao.mapper.JacksonDocumentMapper;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.sql.Date;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractMongoDao<M,K,I> extends AbstractDao<M,K,I> {
    private final MongoCollection<Document> collection;
    private final DocumentMapper<M> mapper;
    public static final String ID_FIELD = "_id";

    public AbstractMongoDao(Class<M> modelClass, MongoCollection<Document> collection) {
        super(modelClass);
        this.collection = collection;
        this.mapper = new JacksonDocumentMapper<>(modelClass,
                JacksonDocumentMapper.DEFAULT_OBJECT_MAPPER,
                getFieldData());
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
            Map<String,Field> all = new HashMap<>();
            all.putAll(fieldData.getAll());
            all.put(id.getName(), id);
            fieldData = fieldData.toBuilder().id(id).all(all).build();
        }
        return fieldData;
    }

    public AbstractMongoDao(Class<M> modelClass, MongoCollection<Document> collection, DocumentMapper<M> mapper) {
        super(modelClass);
        this.collection = collection;
        this.mapper = mapper;
    }

    public MongoCollection<Document> getCollection() {
        return collection;
    }

    protected DocumentMapper<M> getMapper() {
        return mapper;
    }

    public Object generateId() {
        return new ObjectId();
    }

    @Override
    public Optional<M> loadOpt(K id) {
        Document query = new Document(ID_FIELD, id);
        Document doc = collection.find(query).limit(1).first();
        return Optional.ofNullable(mapper.toObject(doc));
    }

    protected M loadByQuery(Document query) {
        Document doc = collection.find(query).limit(1).first();
        if (doc == null) {
            throw new DataNotFoundException(getDisplayModelName() + " not found");
        }
        return mapper.toObject(doc);
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
            mostRecentObject = mapper.toObject(doc);
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
     *    findWithFieldPagination(getCollection.find(query).sort(sort), "email", String.class, 20)
     * Will return the next 20 results starting with "bob@example.com"
     *
     * @param iterable  query
     * @param nextField the name of the field to extract as the "next" item
     * @param nextFieldType the class of the expected value of the "next" field (*as it is stored in MongoDB*)
     * @param limit number of results to limit
     * @return  results containing up to limit results in the query, and the value of the "next" field
     */
    protected <N> Results<M, N> findWithFieldPagination(FindIterable<Document> iterable, String nextField, Class<N> nextFieldType, int limit) {
        ImmutableList.Builder<M> builder = ImmutableList.builder();
        M mostRecentObject = null;
        N mostRecentNext = null;

        // query for one more object than we actually need, in order to determine whether there is a "next" page
        int foundCount = 0;
        for (Document doc : iterable.limit(limit + 1)) {
            if (mostRecentObject != null) {
                builder.add(mostRecentObject);
            }
            mostRecentNext = doc.get(nextField, nextFieldType);
            mostRecentObject = mapper.toObject(doc);
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

    @Override
    public M create(M model, Instant createAt, I createBy) {
        Document doc = mapper.toDocument(model);
        if (doc.get(ID_FIELD) == null) {
            doc.put(ID_FIELD, generateId());
        }
        FieldData fieldData = getFieldData();
        if (createAt != null && fieldData.getCreatedAt() != null) {
            doc.put(fieldData.getCreatedAt().getStorageName(), Date.from(createAt));
        }
        if (createBy != null && fieldData.getCreatedBy() != null) {
            doc.put(fieldData.getCreatedBy().getStorageName(), createBy);
        }
        collection.insertOne(doc);
        return mapper.toObject(doc);
    }

    @Override
    public void update(K id, Update<M> update, Instant updateAt, I updateBy) {
        Document query = new Document(ID_FIELD, id);
        Document upDoc = toUpdateDocument(update, updateAt, updateBy);
        collection.updateOne(query, upDoc);
    }

    @Override
    public M updateAndReturn(K id, Update<M> update, Instant updateAt, I updateBy) {
        Document upDoc = toUpdateDocument(update, updateAt, updateBy);
        Document query = new Document(ID_FIELD, id);
        Document found = getCollection().findOneAndUpdate(query, upDoc);
        return mapper.toObject(found);
    }

    protected Document toUpdateDocument(Update<M> update, Instant updateAt, I updateBy) {
        Document doc = new Document();

        Document set = mapper.toDocument(update.getSetObject());
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
        collection.deleteOne(query);
    }

    @Override
    public Iterable<M> iterateAll() {
        return collection.find().map((d) -> mapper.toObject(d));
    }

    @Override
    public void initTable() {
        for (IndexModel index : getIndexes()) {
            collection.createIndex(index.getKeys(), index.getOptions());
        }
    }

    @Override
    public void dropTable() {
        collection.drop();
    }

    public List<IndexModel> getIndexes() {
        return ImmutableList.of();
    }
}
