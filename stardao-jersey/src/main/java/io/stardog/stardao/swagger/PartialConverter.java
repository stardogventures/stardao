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
 *
 * Optionally, you can convert any other prefix, such as Dto.
 */
public class PartialConverter implements ModelConverter {
    private final String prefix;

    public PartialConverter() {
        this("Partial");
    }

    public PartialConverter(String prefix) {
        this.prefix = prefix;
    }

    public Property resolveProperty(Type type, ModelConverterContext context, Annotation[] annotations, Iterator<ModelConverter> chain) {
        JavaType jType = Json.mapper().constructType(type);
        if(jType != null) {
            Class<?> cls = jType.getRawClass();
            if (cls.getSimpleName().startsWith(prefix)) {
                String baseClass = cls.getSimpleName().substring(prefix.length());
                return new RefProperty("#/definitions/" + baseClass);
            }
        }

        return chain.hasNext() ? chain.next().resolveProperty(type, context, annotations, chain) : null;
    }

    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    }
}
