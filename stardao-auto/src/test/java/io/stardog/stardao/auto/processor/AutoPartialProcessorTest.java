package io.stardog.stardao.auto.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.Assert.*;

public class AutoPartialProcessorTest {
    @Test
    public void testGeneratedPartial() throws Exception {
        PartialTestUser user = PartialTestUser.builder()
                .name("Ian White")
                .age(36)
                .email("example@example.com")
                .build();
        assertEquals("Ian White", user.getName().get());
        assertEquals(new Integer(36), user.getAge().get());
        assertEquals("example@example.com", user.getEmail().get());

        // can convert to builder and remove fields
        user = user.toBuilder().name(Optional.empty()).build();
        assertFalse(user.getName().isPresent());

        // ensure that getter annotations get copied over
        Method getEmail = PartialTestUser.class.getMethod("getEmail");
        assertNotNull(getEmail.getAnnotation(TestAnnotation.class));
    }

    @Test
    public void testJackson() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new Jdk8Module());

        PartialTestUser user = mapper.readValue("{\"age\":36,\"email\":\"example@example.com\"}", PartialTestUser.class);
        assertEquals(new Integer(36), user.getAge().get());
        assertEquals("example@example.com", user.getEmail().get());

        user = PartialTestUser.builder().age(36).build();
        String json = mapper.writeValueAsString(user);
        assertEquals("{\"age\":36}", json);
    }
}
