package io.stardog.stardao.core.field;

import io.stardog.stardao.core.TestModel;
import org.junit.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.Assert.*;

public class FieldScannerTest {
    @Test
    public void testScanAnnotations() throws Exception {
        FieldScanner scanner = new FieldScanner();
        FieldData data = scanner.scanAnnotations(TestModel.class);

        assertEquals("id", data.getId().getName());
        assertEquals("updateId", data.getUpdatedBy().getName());
        assertEquals("updateAt", data.getUpdatedAt().getName());
        assertEquals("createId", data.getCreatedBy().getName());
        assertEquals("createAt", data.getCreatedAt().getName());
    }

    @Test
    public void testToFieldName() throws Exception {
        FieldScanner scanner = new FieldScanner();

        assertEquals("loginAt", scanner.toFieldName(TestModel.class.getDeclaredMethod("getLoginAt")));
        assertNull(scanner.toFieldName(TestModel.class.getDeclaredMethod("getLoginAt", long.class)));
    }
}
