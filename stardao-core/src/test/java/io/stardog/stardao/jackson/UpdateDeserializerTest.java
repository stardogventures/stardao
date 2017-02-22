package io.stardog.stardao.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableSet;
import io.stardog.stardao.core.TestModel;
import io.stardog.stardao.core.Update;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UpdateDeserializerTest {

    @Test
    public void testDeserialize() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String json = "{\"name\":\"Marty\",\"birthday\":\"1985-10-26\",\"email\":null,\"country\":\"\"}";
        Update<TestModel> update = mapper.readValue(json, new TypeReference<Update<TestModel>>() {});
        assertEquals("Marty", update.getPartial().getName());
        assertEquals(LocalDate.of(1985, 10, 26), update.getPartial().getBirthday());
        assertNull(update.getPartial().getEmail());
        assertNull(update.getPartial().getCountry());
        assertEquals(ImmutableSet.of("email", "country"), update.getRemoveFields());
    }

    @Test
    public void testDeserializeSingleBool() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String json = "{\"active\":true}";
        Update<TestModel> update = mapper.readValue(json, new TypeReference<Update<TestModel>>() {});
        assertTrue(update.getPartial().getActive());
    }
}