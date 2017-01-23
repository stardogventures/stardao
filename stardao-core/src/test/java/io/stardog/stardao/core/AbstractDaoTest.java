package io.stardog.stardao.core;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class AbstractDaoTest {
    private TestDao dao;

    @Before
    public void setUp() throws Exception {
        this.dao = new TestDao(TestModel.class);
    }

    @Test
    public void testGetModelClass() throws Exception {
        assertEquals(TestModel.class, dao.getModelClass());
    }

    @Test
    public void testGetIdField() throws Exception {
        assertEquals("id", dao.getIdField());
    }

    @Test
    public void testGetCreatedByField() throws Exception {
        assertEquals("createId", dao.getCreatedByField());
    }

    @Test
    public void testGetCreatedAtField() throws Exception {
        assertEquals("createAt", dao.getCreatedAtField());
    }

    @Test
    public void testGetUpdatedByField() throws Exception {
        assertEquals("updateId", dao.getUpdatedByField());
    }

    @Test
    public void testGetUpdatedAtField() throws Exception {
        assertEquals("updateAt", dao.getUpdatedAtField());
    }

    @Test
    public void testGetUpdatableFields() throws Exception {
        Set<String> fields = dao.getUpdatableFields();
        assertEquals(5, fields.size());
        assertTrue(fields.contains("name"));
        assertTrue(fields.contains("email"));
        assertTrue(fields.contains("country"));
        assertTrue(fields.contains("birthday"));
        assertTrue(fields.contains("active"));
    }
}