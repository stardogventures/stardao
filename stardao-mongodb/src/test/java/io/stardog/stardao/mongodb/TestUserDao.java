package io.stardog.stardao.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexModel;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Arrays;
import java.util.List;

public class TestUserDao extends AbstractMongoDao<TestUser,TestUser,ObjectId,ObjectId> {
    public TestUserDao(MongoCollection<Document> collection) {
        super(TestUser.class, TestUser.class, collection);
    }

    @Override
    public List<IndexModel> getIndexes() {
        return Arrays.asList(new IndexModel(new Document("name", 1)));
    }
}
