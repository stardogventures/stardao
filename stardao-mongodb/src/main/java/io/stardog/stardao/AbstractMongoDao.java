package io.stardog.stardao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexModel;
import io.stardog.stardao.core.AbstractDao;
import io.stardog.stardao.core.Update;
import io.stardog.stardao.mapper.DocumentMapper;
import io.stardog.stardao.mapper.JacksonDocumentMapper;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public abstract class AbstractMongoDao<M,K,I> extends AbstractDao<M,K,I> {
    private final MongoCollection<Document> collection;
    private final DocumentMapper<M> mapper;
    public static final String ID_FIELD = "_id";

    public AbstractMongoDao(Class<M> modelClass, MongoCollection<Document> collection) {
        super(modelClass);
        this.collection = collection;
        this.mapper = new JacksonDocumentMapper<M>(modelClass,
                JacksonDocumentMapper.DEFAULT_OBJECT_MAPPER,
                ImmutableMap.of(getIdField(), ID_FIELD));
    }

    public AbstractMongoDao(Class<M> modelClass, MongoCollection<Document> collection, DocumentMapper<M> mapper) {
        super(modelClass);
        this.collection = collection;
        this.mapper = mapper;
    }

    public MongoCollection<Document> getCollection() {
        return collection;
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

    @Override
    public M create(M model, Instant createAt, I createBy) {
        Document doc = mapper.toDocument(model);
        if (doc.get(ID_FIELD) == null) {
            doc.put(ID_FIELD, generateId());
        }
        if (createAt != null && getCreatedAtField() != null) {
            doc.put(getCreatedAtField(), Date.from(createAt));
        }
        if (createBy != null && getCreatedByField() != null) {
            doc.put(getCreatedByField(), createBy);
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
        if (updateAt != null && getUpdatedAtField() != null) {
            set.put(getUpdatedAtField(), Date.from(updateAt));
        }
        if (updateBy != null && getUpdatedByField() != null) {
            set.put(getUpdatedByField(), updateBy);
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
