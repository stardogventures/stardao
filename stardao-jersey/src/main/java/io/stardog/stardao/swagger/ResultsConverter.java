package io.stardog.stardao.swagger;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;

/**
 * The ResultsConverter turns a Results object that contains Partial objects into a Results object that does not
 * contain Partial in the name.
 */
public class ResultsConverter implements ModelConverter {
    private String prefix;

    public ResultsConverter() {
        this("Partial");
    }

    public ResultsConverter(String prefix) {
        this.prefix = prefix;
    }

    public Property resolveProperty(Type type, ModelConverterContext context, Annotation[] annotations, Iterator<ModelConverter> chain) {
        return chain.hasNext() ? chain.next().resolveProperty(type, context, annotations, chain) : null;
    }

    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        JavaType jType = Json.mapper().constructType(type);
        if(jType != null) {
            Class<?> cls = jType.getRawClass();
            if (cls.getName().equals("io.stardog.stardao.core.Results") && jType.containedTypeCount() == 2) {
                String modelType = jType.containedType(0).getRawClass().getSimpleName();
                if (modelType.startsWith(prefix)) {
                    modelType = modelType.substring(prefix.length());
                }
                String nextType = jType.containedType(1).getRawClass().getSimpleName();

                ModelImpl result = new ModelImpl();
                result.name("Results" + modelType + nextType);

                Property dataProperty = new ArrayProperty(new RefProperty("#/definitions/" + modelType));
                dataProperty.setRequired(true);

                Property nextProperty = new StringProperty();
                result.addProperty("data", dataProperty);
                result.addProperty("next", nextProperty);
                return result;
            }
        }

        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    }
}
