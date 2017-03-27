package io.stardog.stardao.jersey.exceptionmappers;

import io.stardog.stardao.exceptions.DataNotFoundException;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.util.Map;

import static org.junit.Assert.*;

public class DataNotFoundExceptionMapperTest {
    @Test
    public void toResponse() throws Exception {
        DataNotFoundExceptionMapper mapper = new DataNotFoundExceptionMapper();
        Response response = mapper.toResponse(new DataNotFoundException("not found"));
        assertEquals(404, response.getStatus());
        assertEquals("application/json", response.getMediaType().toString());
        Map<String,Object> map = (Map<String,Object>)response.getEntity();
        assertEquals(404, map.get("code"));
        assertEquals("not found", map.get("message"));
    }
}
