package io.stardog.stardao.core.field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import io.stardog.stardao.annotations.*;
import kotlin.reflect.KProperty;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FieldScanner {
    /**
     * Scan a model class, looking for annotations
     * @param modelClass    model class
     * @return  field data representing the result of the scan
     */
    public FieldData scanAnnotations(Class modelClass) {
        FieldData.Builder builder = FieldData.builder();
        ImmutableMap.Builder<String,Field> fieldMap = ImmutableMap.builder();

        Map<Class,Field> found = new HashMap<>();
        Set<String> scannedFields = new HashSet<>();

        for (java.lang.reflect.Field field : modelClass.getDeclaredFields()) {
            String fieldName = field.getName();
            addToMap(builder, fieldMap, found, fieldName, field, isFieldOptional(field));
            scannedFields.add(fieldName);
        }
        for (Method method : modelClass.getDeclaredMethods()) {
            String fieldName = toFieldName(method);
            if (fieldName != null && !scannedFields.contains(fieldName)) {
                addToMap(builder, fieldMap, found, fieldName, method, isMethodFieldOptional(method));
            }
        }
        builder.map(fieldMap.build());
        return builder.build();
    }

    protected void addToMap(FieldData.Builder builder, ImmutableMap.Builder<String,Field> fieldMap,
                            Map<Class,Field> found, String fieldName, AccessibleObject method, boolean isOptional) {
        if (method.isAnnotationPresent(JsonIgnore.class)) {
            return;
        }
        String storageName = fieldName;
        if (method.isAnnotationPresent(StorageName.class)) {
            storageName = method.getAnnotation(StorageName.class).value();
        }
        Field field = Field.builder()
                .name(fieldName)
                .storageName(storageName)
                .optional(isOptional)
                .creatable(method.isAnnotationPresent(Creatable.class))
                .updatable(method.isAnnotationPresent(Updatable.class))
                .build();
        fieldMap.put(fieldName, field);

        if (method.isAnnotationPresent(Id.class)) {
            Field prev = found.get(Id.class);
            if (prev != null) {
                throw new IllegalStateException("Multiple @Id annotations present on " + prev.getName() + " and " + field.getName());
            }
            found.put(Id.class, field);
            builder.id(field);
        }
        // assume that fields named "id" are the id unless it's explicitly specified otherwise
        if (fieldName.equals("id") && !found.containsKey(Id.class)) {
            builder.id(field);
        }
        if (method.isAnnotationPresent(CreatedAt.class)) {
            Field prev = found.get(CreatedAt.class);
            if (prev != null) {
                throw new IllegalStateException("Multiple @CreatedAt annotations present on " + prev.getName() + " and " + field.getName());
            }
            found.put(CreatedAt.class, field);
            builder.createdAt(field);
        }
        if (method.isAnnotationPresent(CreatedBy.class)) {
            Field prev = found.get(CreatedBy.class);
            if (prev != null) {
                throw new IllegalStateException("Multiple @CreatedBy annotations present on " + prev.getName() + " and " + field.getName());
            }
            found.put(CreatedBy.class, field);
            builder.createdBy(field);
        }
        if (method.isAnnotationPresent(UpdatedAt.class)) {
            Field prev = found.get(UpdatedAt.class);
            if (prev != null) {
                throw new IllegalStateException("Multiple @UpdatedAt annotations present on " + prev.getName() + " and " + field.getName());
            }
            found.put(UpdatedAt.class, field);
            builder.updatedAt(field);
        }
        if (method.isAnnotationPresent(UpdatedBy.class)) {
            Field prev = found.get(UpdatedBy.class);
            if (prev != null) {
                throw new IllegalStateException("Multiple @UpdatedBy annotations present on " + prev.getName() + " and " + field.getName());
            }
            found.put(UpdatedBy.class, field);
            builder.updatedBy(field);
        }
    }

    /**
     * Given a method, returns the field name, or null if the method does not appear to be a getter
     * @param method    method on the Dao
     * @return  derived field name
     */
    protected String toFieldName(Method method) {
        if (method.getParameterCount() > 0) {
            return null;
        }
        if (method.getName().startsWith("get")) {
            String field = method.getName().substring(3);
            return field.substring(0, 1).toLowerCase() + field.substring(1);
        } else if (method.getName().startsWith("is")) {
            String field = method.getName().substring(2);
            return field.substring(0, 1).toLowerCase() + field.substring(1);
        } else {
            return null;
        }
    }

    protected boolean isFieldOptional(java.lang.reflect.Field field) {
        KProperty<?> property = ReflectJvmMapping.getKotlinProperty(field);
        if (property != null && property.getReturnType().isMarkedNullable()) {
            return true;
        }
        return field.getType().getSimpleName().equals("Optional");
    }

    protected boolean isMethodFieldOptional(Method method) {
        return method.getReturnType().getSimpleName().equals("Optional");
    }
}
