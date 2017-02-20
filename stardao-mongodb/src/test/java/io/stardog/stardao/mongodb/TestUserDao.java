package io.stardog.stardao.mongodb;

import com.mongodb.client.MongoCollection;
import io.stardog.stardao.mongodb.AbstractMongoDao;
import io.stardog.stardao.mongodb.TestUser;
import org.bson.Document;
import org.bson.types.ObjectId;

public class TestUserDao extends AbstractMongoDao<TestUser,ObjectId,ObjectId> {
    public TestUserDao(MongoCollection<Document> collection) {
        super(TestUser.class, collection);
    }
}
