package io.stardog.stardao.jersey.exceptionmappers;

import com.google.common.collect.ImmutableMap;
import io.stardog.stardao.exceptions.DataValidationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class DataValidationExceptionMapper implements ExceptionMapper<DataValidationException> {
    @Override
    public Response toResponse(DataValidationException e) {
        return Response.status(400)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(ImmutableMap.of("error", e.getMessage(), "errors", e.getErrors()))
                .build();
    }
}
