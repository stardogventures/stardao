package io.stardog.stardao.dynamodb;

import com.amazonaws.services.dynamodbv2.document.Item;
import io.stardog.stardao.dynamodb.mapper.ItemMapper;

import java.util.Iterator;

public class DynamoIterator<M> implements Iterator<M> {
    private final Iterator<Item> iterator;
    private final ItemMapper<M> mapper;

    public DynamoIterator(Iterator<Item> iterator, ItemMapper<M> mapper) {
        this.iterator = iterator;
        this.mapper = mapper;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public M next() {
        return mapper.toObject(iterator.next());
    }
}
