# stardao-dynamodb

## Primary keys

By default, the DAO assumes that you have a unique hash key field which contains `UUID`s, marked with an `@Id` annotation on the field. If this is not the case (you're not using UUIDs, or you don't have a unique `@Id` field), that's fine, you need to either:
  - explicitly pass in the id whenever you insert a new object
  - override the `generatePrimaryKeyValue()` method which generates a new primary key id when you don't pass one
  
## `getKeySchema()`, `getAttributeDefinitions()`, and `getGlobalSecondaryIndexes()`

You should always define these three methods to set up your table structure. When a call is made to `initTable()`, any missing indexes will automatically be added.

Example:

```java
    public List<KeySchemaElement> getKeySchema() {
        return Arrays.asList(
                new KeySchemaElement("id", KeyType.HASH)
        );
    }

    public List<AttributeDefinition> getAttributeDefinitions() {
        return Arrays.asList(
                new AttributeDefinition("id", "S"),
                new AttributeDefinition("email", "S")
        );
    }

    public List<GlobalSecondaryIndex> getGlobalSecondaryIndexes() {
        return Arrays.asList(
                new GlobalSecondaryIndex()
                        .withIndexName("email")
                        .withKeySchema(Arrays.asList(
                                new KeySchemaElement("email", KeyType.HASH)
                        ))
                        .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L))
                        .withProjection(new Projection()
                                .withProjectionType(ProjectionType.KEYS_ONLY))
        );
    }
```

## Protected methods

The following protected methods can be useful when writing your Dao subclass implementations. Note that they are all `protected`, so not directly accessible to users of the Dao you writing. The purpose of these methods is just to make writing your publicly-accessible methods easier and less boilerplatey.

### `getCollection()`

Get ahold of the actual `MongoCollection` object from the mongodb-java library with a call to `getCollection()`. From there you're off to the races with the full functionality of the Java driver.

Example:

```java
public void updateSetLoginAt(ObjectId userId, Instant at) {
    Document query = new Document("_id", userId);
    Document update = new Document("$set", new Document("loginAt", Date.from(at)));
    getCollection().update(query, update);
}
```

#### `getModelMapper()` and `getPartialMapper()`

A call to one of these methods will return a `DocumentMapper` which can convert your model POJOs to Documents and vice versa. It will respect field renaming rules that have been set with `@StorageName` annotations.

#### `loadByIndex(String indexName, String key, Object value)` and `loadByIndexOpt`

It's common to want to load a single object by traversing a secondary index. For example, you might want to load a user by their email address.

Example (assuming you have the email field indexed as an index named "email-index");
```
public User loadByEmail(String email) {
  return loadByIndex("email-index", "email", email);
}
```

The standard version throws a `DataNotFoundException` if no item is found. You can use `loadByIndexOpt` to return an empty `Optional` instead.

### Non-paginated queries: `findByIndex(String indexName, QuerySpec spec)` and `scan(ScanSpec spec)`

For issuing arbitrary queries, you generally want to query by a particular index, and occasionally have to perform a scan. In either case, you build a `QuerySpec` or `ScanSpec` and can pass it in to the parent method.

Example:
```
    public Results<User,String> findActiveLoginsBetween(Instant startAt, Instant endAt) {
        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("#status = :status AND loginAt BETWEEN :start AND :end")
                .withNameMap(new NameMap()
                        .with("#status", "status"))
                .withValueMap(new ValueMap()
                        .withString(":status", "ACTIVE")
                        .withNumber(":start", startAt.toEpochMilli())
                        .withNumber(":end", endAt.toEpochMilli())
                );
        return findByIndex("status-buyAt-index", spec);
    }
```
### Paginated queries

Have not been implemented yet. Right now you have to write them yourself. To-do.
