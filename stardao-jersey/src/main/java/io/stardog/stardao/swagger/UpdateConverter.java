package io.stardog.stardao.swagger;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.models.Model;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.Json;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;

/**
 * The UpdateConverter takes references to Update objects and turns them into references to Model objects.
 */
public class UpdateConverter implements ModelConverter {
    public UpdateConverter() {
    }

    public Property resolveProperty(Type type, ModelConverterContext context, Annotation[] annotations, Iterator<ModelConverter> chain) {
        JavaType jType = Json.mapper().constructType(type);
        if(jType != null) {
            Class<?> cls = jType.getRawClass();
            if (cls.getName().equals("io.stardog.stardao.core.Update") && jType.containedTypeCount() > 0) {
                String innerName = jType.containedType(0).getRawClass().getSimpleName();
                if (innerName.startsWith("Partial")) {
                    return new RefProperty("#/definitions/" + innerName.substring(7));
                } else {
                    return new RefProperty("#/definitions/" + innerName);
                }
            }
        }

        return chain.hasNext() ? chain.next().resolveProperty(type, context, annotations, chain) : null;
    }

    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    }
}
