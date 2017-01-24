package io.stardog.stardao.core;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
}