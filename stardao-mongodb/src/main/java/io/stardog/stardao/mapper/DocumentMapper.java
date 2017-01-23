package io.stardog.stardao.mapper;

import org.bson.Document;

public interface DocumentMapper<M> {
    public M toObject(Document document);
    public Document toDocument(M object);
}
