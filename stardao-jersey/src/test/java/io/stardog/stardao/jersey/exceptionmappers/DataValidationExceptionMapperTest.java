package io.stardog.stardao.jersey.exceptionmappers;

import io.stardog.stardao.exceptions.DataValidationException;
import io.stardog.stardao.validation.ValidationError;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DataValidationExceptionMapperTest {
    @Test
    public void toResponse() throws Exception {
        DataValidationExceptionMapper mapper = new DataValidationExceptionMapper();
        DataValidationException exception = new DataValidationException(Arrays.asList(ValidationError.of("field", "is invalid")));

        Response response = mapper.toResponse(exception);
        assertEquals(400, response.getStatus());
        assertEquals("application/json", response.getMediaType().toString());
        Map<String,Object> map = (Map<String,Object>)response.getEntity();
        assertEquals(400, map.get("code"));
        assertEquals("field: is invalid", map.get("message"));
        assertEquals("field", ((List<ValidationError>)map.get("errors")).get(0).getField());
    }
}
