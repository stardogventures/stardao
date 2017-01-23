package io.stardog.stardao.dynamodb.mapper;

import com.amazonaws.services.dynamodbv2.document.Item;

public interface ItemMapper<M> {
    public M toObject(Item item);
    public Item toItem(M object);
}
