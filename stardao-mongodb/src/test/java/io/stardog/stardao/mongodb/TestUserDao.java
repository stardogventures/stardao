package io.stardog.stardao.mongodb;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;

public class TestUserDao extends AbstractMongoDao<TestUser,TestUser,ObjectId,ObjectId> {
    public TestUserDao(MongoCollection<Document> collection) {
        super(TestUser.class, TestUser.class, collection);
    }
}
