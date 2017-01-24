package io.stardog.stardao.dynamodb;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.google.common.collect.ImmutableMap;
import io.stardog.stardao.core.field.FieldData;
import io.stardog.stardao.dynamodb.mapper.JacksonItemMapper;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class JacksonItemMapperTest {
    @Test
    public void testToItem() throws Exception {
        JacksonItemMapper<TestObject> mapper = new JacksonItemMapper<>(TestObject.class, FieldData.builder().all(ImmutableMap.of()).build());

        UUID id = UUID.randomUUID();
        Instant at = Instant.now();
        LocalDate date = LocalDate.of(2016, 5, 12);
        TestObject object = new TestObject(id, "MyName", at, date, 372);
        Item item = mapper.toItem(object);
        assertEquals(id.toString(), item.get("id"));
        assertEquals("MyName", item.get("name"));
        assertEquals(at.toEpochMilli(), ((Number) item.get("at")).longValue());
        assertEquals("2016-05-12", item.get("date"));
        assertEquals(372, ((Number) item.get("num")).intValue());
    }

    @Test
    public void testToObject() throws Exception {
        JacksonItemMapper<TestObject> mapper = new JacksonItemMapper<>(TestObject.class, FieldData.builder().all(ImmutableMap.of()).build());

        TestObject test = mapper.getObjectMapper().readValue("{\"at\":1479334772334}", TestObject.class);
        assertEquals("2016-11-16T22:19:32.334Z", test.getAt().toString());

        UUID id = UUID.randomUUID();
        Instant at = Instant.now();
        LocalDate date = LocalDate.of(2016, 5, 12);
        TestObject object1 = new TestObject(id, "MyName", at, date, 372);
        Item item = mapper.toItem(object1);
        TestObject object2 = mapper.toObject(item);

        assertEquals(id, object2.getId());
        assertEquals(at, object2.getAt());
        assertEquals(date, object2.getDate());
        assertEquals("MyName", object2.getName());
        assertEquals(new Integer(372), object2.getNum());
    }
}
