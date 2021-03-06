package io.stardog.stardao.core;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.stardog.stardao.jackson.UpdateDeserializer;
import io.stardog.stardao.jackson.UpdateSerializer;

import java.util.Set;

@AutoValue
@JsonDeserialize(using = UpdateDeserializer.class)
@JsonSerialize(using = UpdateSerializer.class)
public abstract class Update<P> {
    public abstract P getPartial();
    public abstract Set<String> getSetFields();
    public abstract Set<String> getRemoveFields();

    public static <T> Update<T> of(T setObject, Set<String> setFields) {
        return new AutoValue_Update<>(setObject, setFields, ImmutableSet.of());
    }

    public static <T> Update<T> of(T setObject, Set<String> setFields, Set<String> removeFields) {
        return new AutoValue_Update<>(setObject, setFields, removeFields);
    }

    public boolean isEmpty() {
        return getSetFields().isEmpty() && getRemoveFields().isEmpty();
    }

    public Set<String> getUpdateFields() {
        return Sets.union(getSetFields(), getRemoveFields());
    }

    public boolean isUpdateField(String field) {
        return getSetFields().contains(field) || getRemoveFields().contains(field);
    }
}
