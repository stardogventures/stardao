package io.stardog.stardao.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.stardog.stardao.core.Results;
import io.stardog.stardao.core.Update;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AbstractDynamoDaoTest {
    private AmazonDynamoDB dynamodb;
    private TestDynamoDao dao;

    // fairly hacky, would prefer a better way to obtain this that works reliably in both mvn and intellij
    private static String getNativeLibsPath() {
        String userPath = System.getProperty("user.dir");
        if (userPath.endsWith("/stardao-dynamodb")) {
            return userPath + "/native-libs";
        } else {
            return userPath + "/stardao-dynamodb/native-libs";
        }
    }

    @Before
    public void setUp() throws Exception {
        System.setProperty("sqlite4java.library.path", getNativeLibsPath());
        dynamodb = DynamoDBEmbedded.create();
        dao = new TestDynamoDao(dynamodb);
        dao.dropAndInitTable();
    }

    @Test
    public void testGetAnnotatedFields() throws Exception {
        assertEquals("id", dao.getFieldData().getId().getName());
        assertEquals("createId", dao.getFieldData().getCreatedBy().getName());
        assertEquals("createAt", dao.getFieldData().getCreatedAt().getName());
        assertEquals("updateId", dao.getFieldData().getUpdatedBy().getName());
        assertEquals("updateAt", dao.getFieldData().getUpdatedAt().getName());
    }

    @Test
    public void testGetTableName() throws Exception {
        assertEquals("test", dao.getTableName());
    }

    @Test
    public void testGetTable() throws Exception {
        assertEquals("test", dao.getTable().getTableName());
    }

    @Test
    public void testGeneratePrimaryKeyValue() throws Exception {
        assertTrue(dao.generatePrimaryKeyValue() instanceof UUID);
    }

    @Test
    public void testToPrimaryKey() throws Exception {
        UUID id = UUID.randomUUID();
        PrimaryKey key = dao.toPrimaryKey(id);
        assertEquals(1, key.getComponents().size());
        for (KeyAttribute attr : key.getComponents()) {
            assertEquals("id", attr.getName());
            assertEquals(id.toString(), attr.getValue());
        }
    }

    @Test
    public void testToPropertyValue() throws Exception {
        assertNull(dao.toStorageValue(null));

        assertEquals(1, dao.toStorageValue(1));

        assertEquals("test", dao.toStorageValue("test"));

        UUID id = UUID.randomUUID();
        assertEquals(id.toString(), dao.toStorageValue(id));

        Instant time = Instant.now();
        assertEquals(time.toEpochMilli(), dao.toStorageValue(time));
    }

    @Test
    public void testCreateLoad() throws Exception {
        UUID creatorId = UUID.randomUUID();
        TestModel created = dao.create(TestModel.builder()
                .name("Ian White")
                .birthday(LocalDate.of(1980, 5, 12))
                .build(), creatorId);
        TestModel loaded = dao.load(created.getId());
        assertEquals(created, loaded);
        assertNotNull(created.getCreateAt());
        assertEquals(creatorId, created.getCreateId());
    }

    @Test
    public void testCreateLoadOpt() throws Exception {
        UUID creatorId = UUID.randomUUID();
        TestModel created = dao.create(TestModel.builder()
                .name("Ian White")
                .birthday(LocalDate.of(1980, 5, 12))
                .build(), creatorId);
        TestModel loaded = dao.loadOpt(created.getId()).get();

        Optional<TestModel> empty = dao.loadOpt(UUID.randomUUID());
        assertFalse(empty.isPresent());
    }

    @Test
    public void testLoadByIndex() throws Exception {
        TestModel created = dao.create(TestModel.builder()
                .name("Ian White")
                .email("ian@example.com")
                .birthday(LocalDate.of(1980, 5, 12))
                .build());
        TestModel loaded = dao.loadByIndex("email", "email", "ian@example.com");
        assertEquals(created, loaded);
    }

    @Test
    public void testIterateAll() throws Exception {
        dao.create(TestModel.builder()
                .name("Ian White")
                .email("ian@example.com")
                .build());
        dao.create(TestModel.builder()
                .name("Bob Smith")
                .email("bob@example.com")
                .build());

        int count = 0;
        for (TestModel m : dao.iterateAll()) {
            count++;
        }
        assertEquals(2, count);
    }

    private void populateSampleData() {
        dao.create(TestModel.builder()
                .name("Ian White")
                .email("ian@example.com")
                .build());
        dao.create(TestModel.builder()
                .name("Bob Smith")
                .email("bob@example.com")
                .build());
    }

    @Test
    public void testScanAll() throws Exception {
        populateSampleData();
        Results<TestModel,UUID> results = dao.scanAll();
        assertEquals(2, results.getData().size());
    }

    @Test
    public void testScan() throws Exception {
        populateSampleData();
        ScanSpec spec = new ScanSpec()
                .withFilterExpression("#name = :name")
                .withNameMap(new NameMap().with("#name", "name"))
                .withValueMap(new ValueMap().with(":name", "Ian White"));
        Results<TestModel,UUID> results = dao.scan(spec);
        assertEquals("Ian White", results.getData().get(0).getName());
        assertEquals(1, results.getData().size());
    }

    @Test
    public void testFindByIndex() throws Exception {
        populateSampleData();
        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("email = :email")
                .withValueMap(new ValueMap().with(":email", "bob@example.com"));
        Results<TestModel,UUID> results = dao.findByIndex("email", spec);
        assertEquals("bob@example.com", results.getData().get(0).getEmail());
        assertEquals(1, results.getData().size());
    }

    @Test
    public void testCheckUniqueField() throws Exception {
        populateSampleData();
        assertTrue(dao.checkUniqueField("email", "email", "new@example.com", null));
        assertFalse(dao.checkUniqueField("email", "email", "bob@example.com", null));
        TestModel bob = dao.loadByIndex("email", "email", "bob@example.com");
        assertTrue(dao.checkUniqueField("email", "email", "bob@exmaple.com", bob.getId()));
    }

    @Test
    public void testToCreateItem() throws Exception {
        Instant now = Instant.now();
        UUID creatorId = UUID.randomUUID();
        Item item = dao.toCreateItem(TestModel.builder().name("Ian White").build(), now, creatorId);
        assertNotNull(item.get("id"));
        assertEquals(creatorId.toString(), item.get("createId"));
        assertEquals(new BigDecimal(now.toEpochMilli()), item.get("createAt"));
    }

    @Test
    public void testUpdate() throws Exception {

    }

    @Test
    public void testUpdateAndReturn() throws Exception {

    }

    @Test
    public void testToUpdateItemSpec() throws Exception {
        UUID updateId = UUID.randomUUID();
        UUID updaterId = UUID.randomUUID();
        Instant now = Instant.now();
        Update<TestModel> update = Update.of(
                TestModel.builder().name("Test").birthday(LocalDate.of(1985, 10, 26)).build(),
                ImmutableSet.of("name", "birthday"),
                ImmutableSet.of("email"));
        UpdateItemSpec spec = dao.toUpdateItemSpec(updateId, update, now, updaterId);
        assertEquals("SET #name = :name, #birthday = :birthday, #updateId = :updateId, #updateAt = :updateAt REMOVE #email", spec.getUpdateExpression());
        assertEquals(updaterId.toString(), spec.getValueMap().get(":updateId"));
        assertEquals("Test", spec.getValueMap().get(":name"));
        assertEquals(updaterId.toString(), spec.getValueMap().get(":updateId"));
        assertEquals(new BigDecimal(now.toEpochMilli()), spec.getValueMap().get(":updateAt"));
    }

    @Test
    public void testDelete() throws Exception {

    }

    @Test
    public void testGetKeySchema() throws Exception {

    }

    @Test
    public void testGetAttributeDefinitions() throws Exception {

    }

    @Test
    public void testGetGlobalSecondaryIndexes() throws Exception {

    }

    @Test
    public void testCreateTable() throws Exception {

    }

    @Test
    public void testDeleteTable() throws Exception {

    }

    @Test
    public void testCopyTable() throws Exception {

    }

    @Test
    public void testEnsureIndexes() throws Exception {

    }

    @Test
    public void testGetAttribute() throws Exception {

    }

    @Test
    public void testNoIdField() throws Exception {
        NoIdModel model = new NoIdModel();
        model.name = "Test";

        NoIdDao noIdDao = new NoIdDao(NoIdModel.class, dynamodb, "noid");
        noIdDao.dropAndInitTable();
        NoIdModel created = noIdDao.create(model);
        assertEquals(model.name, created.name);
    }

    private class NoIdDao extends AbstractDynamoDao<NoIdModel,NoIdModel,Void,UUID> {
        public NoIdDao(Class<NoIdModel> modelClass, AmazonDynamoDB db, String tableName) {
            super(modelClass, modelClass, db, tableName);
        }

        @Override
        public List<KeySchemaElement> getKeySchema() {
            return ImmutableList.of(new KeySchemaElement("name", KeyType.HASH));
        }

        @Override
        public List<AttributeDefinition> getAttributeDefinitions() {
            return ImmutableList.of(new AttributeDefinition("name", "S"));
        }
    }

    private static class NoIdModel {
        public String name;
    }
}