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
 * The PartialConverter takes references to PartialModel objects and turns them into references to Model objects.
 */
public class PartialConverter implements ModelConverter {
    public PartialConverter() {
    }

    public Property resolveProperty(Type type, ModelConverterContext context, Annotation[] annotations, Iterator<ModelConverter> chain) {
        JavaType jType = Json.mapper().constructType(type);
        if(jType != null) {
            Class<?> cls = jType.getRawClass();
            if (cls.getSimpleName().startsWith("Partial")) {
                String baseClass = cls.getSimpleName().substring(7);
                return new RefProperty("#/definitions/" + baseClass);
            }
        }

        return chain.hasNext() ? chain.next().resolveProperty(type, context, annotations, chain) : null;
    }

    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    }
}
