package io.stardog.stardao.validation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.stardog.stardao.core.TestModel;
import io.stardog.stardao.core.Update;
import io.stardog.stardao.core.field.FieldData;
import io.stardog.stardao.core.field.FieldScanner;
import io.stardog.stardao.exceptions.DataValidationException;
import io.stardog.stardao.jackson.JsonHelper;
import org.junit.Before;
import org.junit.Test;

import javax.validation.Validation;
import javax.validation.groups.Default;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;

public class ModelValidatorTest {
    private ModelValidator validator;
    private FieldData fieldData;

    @Before
    public void setUp() throws Exception {
        this.validator = new ModelValidator(Validation.buildDefaultValidatorFactory().getValidator(),
                new ObjectMapper()
                        .registerModule(new Jdk8Module()));
        FieldScanner scanner = new FieldScanner();
        fieldData = scanner.scanAnnotations(TestValidateModel.class);
    }

    @Test
    public void testGetModelValidationErrors() throws Exception {
        List<ValidationError> errors = validator.getModelValidationErrors(null, Default.class);
        assertEquals(1, errors.size());
        assertEquals("object is null", errors.get(0).getMessage());

        TestValidateModel model = TestValidateModel.builder().email("invalid").build();
        errors = validator.getModelValidationErrors(model, Default.class);
        assertEquals(1, errors.size());
        assertEquals("must be a well-formed email address", errors.get(0).getMessage());

        model = TestValidateModel.builder().email("example@example.com").build();
        errors = validator.getModelValidationErrors(model, Default.class);
        assertEquals(0, errors.size());
    }

    @Test
    public void testGetCreateValidationErrors() throws Exception {
        List<ValidationError> errors = validator.getCreateValidationErrors(null, fieldData);
        assertEquals(1, errors.size());

        TestValidateModel model = TestValidateModel.builder().build();
        errors = validator.getCreateValidationErrors(model, fieldData);
        assertEquals(2, errors.size());
        assertEquals("is required", errors.get(0).getMessage());
        assertEquals("is required", errors.get(1).getMessage());

        model = TestValidateModel.builder().email("invalid").build();
        errors = validator.getCreateValidationErrors(model, fieldData);
        assertEquals(2, errors.size());
        assertEquals("is required", errors.get(0).getMessage());
        assertEquals("must be a well-formed email address", errors.get(1).getMessage());

        model = TestValidateModel.builder().email("example@example.com").type("type").loginAt(Instant.now()).build();
        errors = validator.getCreateValidationErrors(model, fieldData);
        assertEquals(1, errors.size());
        assertEquals("loginAt", errors.get(0).getField());
        assertEquals("is not creatable", errors.get(0).getMessage());
    }

    @Test
    public void testGetUpdateValidationErrors() throws Exception {
        List<ValidationError> errors = validator.getUpdateValidationErrors(null, fieldData);
        assertEquals(1, errors.size());

        Update<TestValidateModel> update = JsonHelper.update("{email:'bad'}", TestValidateModel.class);
        errors = validator.getUpdateValidationErrors(update, fieldData);
        assertEquals(1, errors.size());
        assertEquals("must be a well-formed email address", errors.get(0).getMessage());

        // attempt to modify a non-Updatable field
        update = JsonHelper.update("{type:'change'}", TestValidateModel.class);
        errors = validator.getUpdateValidationErrors(update, fieldData);
        assertEquals(1, errors.size());
        assertEquals("is not updatable", errors.get(0).getMessage());

        // attempt to unset a non-optional field
        update = JsonHelper.update("{email:null}", TestValidateModel.class);
        errors = validator.getUpdateValidationErrors(update, fieldData);
        assertEquals(1, errors.size());
        assertEquals("email: is required", errors.get(0).toString());
    }

    @Test
    public void testValidateModel() throws Exception {
        try {
            validator.validateModel(TestValidateModel.builder().email("bad").build());
            fail("Expected DataValidationException");
        } catch (DataValidationException e) {
            assertEquals(1, e.getErrors().size());
        }

        assertTrue(validator.validateModel(TestValidateModel.builder().email("ok@example.com").build()));
    }

    @Test
    public void testValidateCreate() throws Exception {
        try {
            validator.validateCreate(TestValidateModel.builder().email("ok@example.com").build(), fieldData);
            fail("Expected DataValidationException");
        } catch (DataValidationException e) {
            assertEquals(1, e.getErrors().size());
        }

        assertTrue(validator.validateCreate(TestValidateModel.builder().email("ok@example.com").type("type").build(), fieldData));
    }

    @Test
    public void testValidateUpdate() throws Exception {
        try {
            validator.validateUpdate(JsonHelper.update("{email:'bad'}", TestValidateModel.class), fieldData);
            fail("Expected DataValidationException");
        } catch (DataValidationException e) {
            assertEquals(1, e.getErrors().size());
        }

        assertTrue(validator.validateUpdate(JsonHelper.update("{email:'ok@example.com'}", TestValidateModel.class), fieldData));

    }

}
