package io.stardog.stardao.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TestDynamoDao extends AbstractDynamoDao<TestModel,UUID,UUID> {
    private boolean emptyIndexes;

    public TestDynamoDao(AmazonDynamoDB db) {
        super(TestModel.class, db, "test");
    }

    public void setEmptyIndexes(boolean emptyIndexes) {
        this.emptyIndexes = emptyIndexes;
    }

    @Override
    public List<KeySchemaElement> getKeySchema() {
        return Arrays.asList(
                new KeySchemaElement("id", KeyType.HASH)
        );
    }

    @Override
    public List<AttributeDefinition> getAttributeDefinitions() {
        if (emptyIndexes) {
            return Arrays.asList(
                    new AttributeDefinition("id", "S")
            );
        }
        return Arrays.asList(
                new AttributeDefinition("id", "S"),
                new AttributeDefinition("orgId", "S"),
                new AttributeDefinition("email", "S"),
                new AttributeDefinition("createAt", "N")
        );
    }

    public List<GlobalSecondaryIndex> getGlobalSecondaryIndexes() {
        if (emptyIndexes) {
            return Arrays.asList();
        }
        return Arrays.asList(
                new GlobalSecondaryIndex()
                        .withIndexName("orgId_createAt")
                        .withKeySchema(Arrays.asList(
                                new KeySchemaElement("orgId", KeyType.HASH),
                                new KeySchemaElement("createAt", KeyType.RANGE)
                        ))
                        .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L))
                        .withProjection(new Projection()
                                .withProjectionType(ProjectionType.ALL)),
                new GlobalSecondaryIndex()
                        .withIndexName("email")
                        .withKeySchema(Arrays.asList(
                                new KeySchemaElement("email", KeyType.HASH)
                        ))
                        .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L))
                        .withProjection(new Projection()
                                .withProjectionType(ProjectionType.ALL))
        );
    }
}