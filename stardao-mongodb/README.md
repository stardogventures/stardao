# stardao-mongodb

The `AbstractMongoDao` leverages [MongoJack](http://mongojack.org/)'s modelMapper. However, it does not use any other MongoJack features.

This library is based on the post-3.0 Java driver and therefore uses `MongoCollection<Document>`, `Document`, and the other good stuff from 3.0 rather than `DBCollection` and `BasicDBObject`.

## MongoDB _ids

MongoDB mandates that every object have an _id field. Therefore, whichever field you have marked with `@Id` is automatically treated as if you had annotated `@StorageField("_id")` -- it will always be stored as `_id` regardless of what the POJO field name is.

## Protected methods

The following protected methods can be useful when writing your Dao subclass implementations.

### getCollection()

Get ahold of the actual `MongoCollection` object from the mongodb-java library with a call to `getCollection()`. From there you're off to the races with the full functionality of the Java driver.

Example:

```java
public void updateSetLoginAt(ObjectId userId, Instant at) {
    Document query = new Document("_id", userId);
    Document update = new Document("$set", new Document("loginAt", Date.from(at)));
    getCollection().update(query, update);
}
```

#### getMapper()

A call to `getMapper()` will return a `DocumentMapper` which can convert POJOs to Documents and vice versa. It will respect field renaming rules that have been set with `@StorageName` annotations.

Example:

#### loadByQuery(Document query)

If you want to write a method that loads a single object by a non-id field, you can call `loadByQuery()`

Example:

```java
public User loadByEmail(String email) {
    return loadByQuery(new Document("email", email));
}
```

### Paginated queries: findWithFieldPagination() and findWithSkipLimitPagination()

There are two built-in ways to write methods that perform paginated queries. Both ways take a `FindIterable<Document>` and return a `Results` object.

#### findWithFieldPagination(FindIterable<Document> query, String fieldName, Class fieldType, int limit)

The preferred way is to use `findWithFieldPagination()`. This method is useful when traversing a sorted index. It assumes that you are using a $gte / $lte operation as part of your query. If using an index, it will dramatically outperform `findWithSkipLimitPagination()`

For example, you might want to paginate through a sorted list of users by their email address:

```
public Results<User,String> findByOrgIdSortByEmail(ObjectId orgId, String from, int limit) {
    Document query = new Document("orgId", orgId);
    if (from != null) {
        query = query.append("email", new Document("$gte", from));
    }
    Document sort = new Document("email", 1);
    return findWithFieldPagination(getCollection().find(query).sort(sort), "email", String.class, limit);
}
```

Each result will return the email address of the "next" user in the `next` field. The caller can pass that "next" as the "from" parameter to the next call.

This will perform much better than using "skip" because we leverage the index on email.

#### findWithSkipLimitPagination(FindIterable<Document> query, int skip, int limit)

In some circumstances, you have to use skip. Be advised that MongoDB will traverse the documents it is "skipping", so you might really kill performance with large skip values.

```
public Results<User,Integer> findByOrgId(ObjectId orgId, int from, int limit) {
    Document query = new Document("orgId", orgId);
    Document sort = new Document("email", 1);
    return findWithSkipLimitPagination(getCollection().find(query).sort(sort), from, limit);
}
```

In this case, the next skip value to use will be placed in the `next` field of the `Results`.

The client can perform the exact same approach of examining the `next` field, and passing it as the following `from`.
