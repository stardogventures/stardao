package io.stardog.stardao.mongodb;

import com.github.fakemongo.Fongo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.stardog.stardao.core.Results;
import io.stardog.stardao.core.Update;
import io.stardog.stardao.exceptions.DataNotFoundException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.geojson.Point;
import org.junit.Before;
import org.junit.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.Assert.*;

public class AbstractMongoDaoTest {
    private TestUserDao dao;

    @Before
    public void setUp() throws Exception {
        Fongo fongo = new Fongo("fake-mongo");
        dao = new TestUserDao(fongo.getMongo().getDatabase("test-mongo").getCollection("test-user"));

    }

    @Test
    public void testGetCollectionName() throws Exception {
        assertEquals("test-user", dao.getCollectionName());
    }

    @Test
    public void testIdHasCorrectStorageName() throws Exception {
        assertEquals("id", dao.getFieldData().getId().getName());
        assertEquals("_id", dao.getFieldData().getId().getStorageName());
    }

    @Test
    public void testGetCollection() throws Exception {
        assertEquals("test-user", dao.getCollection().getNamespace().getCollectionName());
    }

    @Test
    public void testGenerateId() throws Exception {
        assertTrue(dao.generateId() instanceof ObjectId);
    }

    @Test
    public void testCreateAndLoadOpt() throws Exception {
        TestUser created = dao.create(TestUser.builder().name("Ian").build());

        TestUser load = dao.loadOpt(created.getId()).get();
        assertEquals(load, created);
    }

    @Test
    public void testLoadByQuery() throws Exception {
        TestUser created = dao.create(TestUser.builder().name("Ian").email("ian@example.com").build());

        TestUser load = dao.loadByQuery(new Document("email", "ian@example.com"));
        assertEquals(load, created);

        try {
            dao.loadByQuery(new Document("email", "notfound@example.com"));
            fail("Expected DataNotFoundException");
        } catch (DataNotFoundException e) {
            assertEquals("TestUser not found", e.getMessage());
        }
    }

    @Test
    public void testLoadByQuerySort() throws Exception {
        TestUser created1 = dao.create(TestUser.builder().name("Ian").email("ian1@example.com").build());
        TestUser created2 = dao.create(TestUser.builder().name("Ian").email("ian2@example.com").build());

        TestUser load = dao.loadByQuery(new Document("name", "Ian"), new Document("email", 1));
        assertEquals(created1, load);
    }

    @Test
    public void testLoadByQueryOpt() throws Exception {
        TestUser created = dao.create(TestUser.builder().name("Ian").email("ian@example.com").build());

        TestUser load = dao.loadByQueryOpt(new Document("email", "ian@example.com")).get();
        assertEquals(load, created);

        assertFalse(dao.loadByQueryOpt(new Document("email", "bob@example.com")).isPresent());
    }

    @Test
    public void testFindByQuery() throws Exception {
        TestUser created1 = dao.create(TestUser.builder().name("Ian").email("ian1@example.com").build());
        TestUser created2 = dao.create(TestUser.builder().name("Ian").email("ian2@example.com").build());

        Results<TestUser,ObjectId> results = dao.findByQuery(new Document("name", "Ian"), new Document("email", -1));
        assertEquals(2, results.getData().size());
        assertEquals(created2, results.getData().get(0));
        assertEquals(created1, results.getData().get(1));
    }

    @Test
    public void testFindWithSkipLimitPagination() throws Exception {
        for (int i=0; i < 100; i++) {
            dao.create(TestUser.builder().name("Bob " + String.format("%02d", i)).active(true).build());
        }
        Document query = new Document("active", true);
        Document sort = new Document("name", 1);

        Results<TestUser,Integer> page1 = dao.findWithSkipLimitPagination(dao.getCollection().find(query).sort(sort), 0, 50);
        assertEquals(50, page1.getData().size());
        assertEquals("Bob 00", page1.getData().get(0).getName());
        assertEquals("Bob 49", page1.getData().get(49).getName());
        assertEquals(new Integer(50), page1.getNext().get());

        Results<TestUser,Integer> page2 = dao.findWithSkipLimitPagination(dao.getCollection().find(query).sort(sort), 50, 50);
        assertEquals(50, page2.getData().size());
        assertEquals("Bob 50", page2.getData().get(0).getName());
        assertEquals("Bob 99", page2.getData().get(49).getName());
        assertFalse(page2.getNext().isPresent());

        page2 = dao.findWithSkipLimitPagination(dao.getCollection().find(query).sort(sort), 50, 1000);
        assertEquals(50, page2.getData().size());
        assertEquals("Bob 50", page2.getData().get(0).getName());
        assertEquals("Bob 99", page2.getData().get(49).getName());
        assertFalse(page2.getNext().isPresent());

        Results<TestUser,Integer> nomatch = dao.findWithSkipLimitPagination(dao.getCollection().find(query).sort(sort), 100, 0);
        assertEquals(0, nomatch.getData().size());
        assertFalse(nomatch.getNext().isPresent());
    }

    @Test
    public void testFindWithFieldPagination() throws Exception {
        for (int i=0; i < 100; i++) {
            dao.create(TestUser.builder().name("Bob " + String.format("%02d", i)).active(true).build());
        }
        Document query = new Document("active", true);
        Document sort = new Document("name", 1);

        Results<TestUser,String> page1 = dao.findWithRangedPagination(dao.getCollection().find(query).sort(sort), "name", String.class, 50);
        assertEquals(50, page1.getData().size());
        assertEquals("Bob 00", page1.getData().get(0).getName());
        assertEquals("Bob 49", page1.getData().get(49).getName());
        assertEquals("Bob 50", page1.getNext().get());

        query = new Document("active", true).append("name", new Document("$gte", "Bob 50"));
        Results<TestUser,String> page2 = dao.findWithRangedPagination(dao.getCollection().find(query).sort(sort), "name", String.class, 50);
        assertEquals(50, page2.getData().size());
        assertEquals("Bob 50", page2.getData().get(0).getName());
        assertEquals("Bob 99", page2.getData().get(49).getName());
        assertFalse(page2.getNext().isPresent());

        page2 = dao.findWithRangedPagination(dao.getCollection().find(query).sort(sort), "name", String.class, 1000);
        assertEquals(50, page2.getData().size());
        assertEquals("Bob 50", page2.getData().get(0).getName());
        assertEquals("Bob 99", page2.getData().get(49).getName());
        assertFalse(page2.getNext().isPresent());

        query = new Document("active", false);
        Results<TestUser,String> nomatch = dao.findWithRangedPagination(dao.getCollection().find(query).sort(sort), "name", String.class, 50);
        assertEquals(0, nomatch.getData().size());
        assertFalse(nomatch.getNext().isPresent());
    }

    @Test
    public void testExists() throws Exception {
        TestUser created = dao.create(TestUser.builder().name("Ian").build());

        assertFalse(dao.exists(new Document("name", "Bob"), null));
        assertTrue(dao.exists(new Document("name", "Ian"), null));
        assertFalse(dao.exists(new Document("name", "Bob"), created.getId()));
    }

    @Test
    public void testCreate() throws Exception {
        Instant now = Instant.now();
        ObjectId creatorId = new ObjectId();
        TestUser created = dao.create(TestUser.builder().name("Ian").build(), now, creatorId);
        assertNotNull(created.getId());
        assertEquals("Ian", created.getName());
        assertEquals(creatorId, created.getCreateId());
        assertEquals(now, created.getCreateAt());
    }

    @Test
    public void testCreateWithFieldsInPlace() throws Exception {
        Instant now = Instant.now();
        ObjectId creatorId = new ObjectId();
        TestUser created = dao.create(TestUser.builder().name("Ian").createAt(now).createId(creatorId).build(), Instant.now(), new ObjectId());
        assertNotNull(created.getId());
        assertEquals("Ian", created.getName());
        assertEquals(creatorId, created.getCreateId());
        assertEquals(now, created.getCreateAt());
    }

    @Test
    public void testUpdate() throws Exception {
        TestUser created = dao.create(TestUser.builder().name("Ian").email("ian@example.com").build());

        ObjectId updateBy = new ObjectId();
        Instant now = Instant.now();

        Update<TestUser> update = Update.of(
                TestUser.builder().name("Bob").build(),
                ImmutableSet.of("name"),
                ImmutableSet.of("email"));
        dao.update(created.getId(), update, now, updateBy);

        TestUser load = dao.load(created.getId());
        assertEquals("Bob", load.getName());
        assertEquals(updateBy, load.getUpdateId());
        assertEquals(now, load.getUpdateAt());
        assertNull(load.getEmail());
    }

    @Test
    public void testUpdateAndReturn() throws Exception {
        TestUser created = dao.create(TestUser.builder().name("Ian").build());

        ObjectId updateBy = new ObjectId();
        Instant now = Instant.now();

        Update<TestUser> update = Update.of(
                TestUser.builder().name("Bob").build(),
                ImmutableSet.of("name"));
        TestUser prev = dao.updateAndReturn(created.getId(), update, now, updateBy);

        assertEquals(created, prev);

        TestUser load = dao.load(created.getId());
        assertEquals("Bob", load.getName());
        assertEquals(updateBy, load.getUpdateId());
        assertEquals(now, load.getUpdateAt());
    }

    @Test
    public void testToUpdateDocument() throws Exception {
        Update<TestUser> update = Update.of(
                TestUser.builder().birthday(LocalDate.of(1980, 5, 12)).build(),
                ImmutableSet.of("birthday"),
                ImmutableSet.of("email"));
        Instant now = Instant.now();
        ObjectId updater = new ObjectId();
        Document doc = dao.toUpdateDocument(update, now, updater);
        Document expected = new Document("$set",
                new Document("birthday", "1980-05-12")
                    .append("updateAt", Date.from(now))
                    .append("updateId", updater))
                .append("$unset", new Document("email", 1));
        assertEquals(expected, doc);
    }

    @Test
    public void testDelete() throws Exception {
        TestUser created = dao.create(TestUser.builder().name("Ian").build());

        dao.delete(created.getId());

        Optional<TestUser> load = dao.loadOpt(created.getId());
        assertFalse(load.isPresent());
    }

    @Test
    public void testCanStoreGeoJson() throws Exception {
        TestUser created = dao.create(TestUser.builder().location(new Point(-73.9857, 40.7484)).build());

        TestUser load = dao.load(created.getId());
        assertEquals(-73.9857, load.getLocation().getCoordinates().getLongitude(), .00001);
    }

    @Test
    public void testIterateAll() throws Exception {
        TestUser created = dao.create(TestUser.builder().name("Ian").email("ian@example.com").build());
        int count = 0;
        for (TestUser user : dao.iterateAll()) {
            assertEquals(created, user);
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    public void testInitTable() throws Exception {
        dao.initTable();
        int count = 0;
        for (Document doc : dao.getCollection().listIndexes()) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testDropTable() throws Exception {
        dao.create(TestUser.builder().name("Ian").build());
        dao.dropTable();
        assertFalse(dao.exists(new Document("name", "Ian"), null));
    }

    @Test
    public void testUpdateOf() throws Exception {
        Update<TestUser> update = dao.updateOf(TestUser.builder().name("Rename").build());
        assertTrue(update.isUpdateField("name"));
        assertEquals(ImmutableSet.of("name"), update.getUpdateFields());
        assertEquals("Rename", update.getPartial().getName());
    }

    @Test
    public void testUpdateOfWithRemoveFields() throws Exception {
        Update<TestUser> update = dao.updateOf(TestUser.builder().name("Rename").build(), ImmutableList.of("email"));
        assertTrue(update.isUpdateField("name"));
        assertTrue(update.isUpdateField("email"));
        assertEquals("Rename", update.getPartial().getName());
        assertEquals(ImmutableSet.of("name"), update.getSetFields());
        assertEquals(ImmutableSet.of("email"), update.getRemoveFields());
    }
}
