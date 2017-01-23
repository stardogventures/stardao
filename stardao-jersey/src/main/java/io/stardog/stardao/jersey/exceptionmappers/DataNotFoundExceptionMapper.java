package io.stardog.stardao.jersey.exceptionmappers;

import com.google.common.collect.ImmutableMap;
import io.stardog.stardao.exceptions.DataNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class DataNotFoundExceptionMapper implements ExceptionMapper<DataNotFoundException> {
    @Override
    public Response toResponse(DataNotFoundException e) {
        return Response.status(404)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(ImmutableMap.of("error", e.getMessage()))
                .build();
    }
}
