package io.stardog.stardao.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableSet;
import io.stardog.stardao.core.TestModel;
import io.stardog.stardao.core.Update;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.*;

public class UpdateSerializerTest {
    @Test
    public void serialize() throws Exception {
        Update<TestModel> update = Update.of(TestModel.builder()
                .email("test@example.com")
                .birthday(LocalDate.of(2017, 3, 1))
                .name("Test")
                .build(),
                ImmutableSet.of("email", "birthday"),
                ImmutableSet.of("country"));

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        String json = mapper.writeValueAsString(update);
        assertEquals("{\"email\":\"test@example.com\",\"birthday\":\"2017-03-01\",\"country\":null}", json);
    }
}
