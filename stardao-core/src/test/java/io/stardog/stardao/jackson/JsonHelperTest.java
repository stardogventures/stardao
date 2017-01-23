package io.stardog.stardao.jackson;

import io.stardog.stardao.core.TestModel;
import io.stardog.stardao.core.Update;
import org.junit.Test;

import static org.junit.Assert.*;

public class JsonHelperTest {

    @Test
    public void testObject() throws Exception {
        TestModel object = JsonHelper.object("{'name':'Ian','active':true}", TestModel.class);
        assertEquals("Ian", object.getName());
    }

    @Test
    public void testUpdate() throws Exception {
        Update<TestModel> update = JsonHelper.update("{'name':'Ian','active':true,email:null}", TestModel.class);
        assertTrue(update.getSetObject().getActive());
        assertTrue(update.getRemoveFields().contains("email"));
    }
}