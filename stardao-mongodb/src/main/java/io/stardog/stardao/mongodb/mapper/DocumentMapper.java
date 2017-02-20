package io.stardog.stardao.mongodb.mapper;

import org.bson.Document;

public interface DocumentMapper<M> {
    public M toObject(Document document);
    public Document toDocument(M object);
}
