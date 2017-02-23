package io.stardog.stardao.mongodb.mapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.stardog.stardao.mongodb.TestAddress;
import io.stardog.stardao.mongodb.TestUser;
import io.stardog.stardao.core.field.Field;
import io.stardog.stardao.core.field.FieldData;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.geojson.Point;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class JacksonDocumentMapperTest {
    private JacksonDocumentMapper<TestUser> mapper;

    @Before
    public void setUp() throws Exception {
        Map<String,Field> fields = ImmutableMap.of("id", Field.builder().name("id").storageName("_id").optional(false).updatable(false).build());
        FieldData fieldData = FieldData.builder().all(fields).build();
        mapper = new JacksonDocumentMapper<>(TestUser.class, JacksonDocumentMapper.DEFAULT_OBJECT_MAPPER, fieldData);
    }

    @Test
    public void testToObject() throws Exception {
        ObjectId id = new ObjectId();
        Document doc = new Document("_id", id)
                .append("name", "Ian White")
                .append("email", "ian@example.com")
                .append("birthday", "1980-05-12")
                .append("active", true)
                .append("count", 5)
                .append("loginAt", new Date(1485116825000L));

        TestUser user = mapper.toObject(doc);
        assertEquals(id, user.getId());
        assertEquals("Ian White", user.getName());
        assertEquals("ian@example.com", user.getEmail());
        assertEquals(LocalDate.of(1980, 5, 12), user.getBirthday());
        assertEquals(1485116825000L, user.getLoginAt().toEpochMilli());
    }

    @Test
    public void testToDocumentAndBack() throws Exception {
        ObjectId id = new ObjectId();
        ObjectId categoryId = new ObjectId();
        ObjectId friend1 = new ObjectId();
        ObjectId friend2 = new ObjectId();

        TestAddress address1 = TestAddress.builder().city("Boston").state("MA").build();
        TestAddress address2 = TestAddress.builder().city("New York").state("NY").build();

        UUID uuid = UUID.randomUUID();
        TestUser user = TestUser.builder()
                .id(id)
                .categoryId(categoryId)
                .name("Ian White")
                .email("ian@example.com")
                .loginAt(Instant.ofEpochMilli(1485116825000L))
                .uuid(uuid)
                .friends(ImmutableList.of(friend1, friend2))
                .addresses(ImmutableList.of(address1, address2))
                .build();
        Document doc = mapper.toDocument(user);
        assertEquals(8, doc.size());
        assertEquals(id, doc.get("_id"));
        assertEquals(categoryId, doc.get("categoryId"));
        assertEquals(friend1, doc.get("friends", List.class).get(0));
        assertEquals(ImmutableMap.of("city", "Boston", "state", "MA"), doc.get("addresses", List.class).get(0));

        TestUser back = mapper.toObject(doc);
        assertEquals(user, back);
    }

    @Test
    public void testMapGeoJsonPoint() throws Exception {
        Point point = new Point(-73.9857, 40.7484);
        JacksonDocumentMapper<Point> pointMapper = new JacksonDocumentMapper<>(Point.class);
        Document doc = pointMapper.toDocument(point);
        assertEquals("Document{{type=Point, coordinates=[-73.9857, 40.7484]}}", doc.toString());
        Point back = pointMapper.toObject(doc);
        assertEquals(point, back);
    }

    @Test
    public void testDbObjectToDocument() throws Exception {

    }

    @Test
    public void testToDbObject() throws Exception {

    }
}
