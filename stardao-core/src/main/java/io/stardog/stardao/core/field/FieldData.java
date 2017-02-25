package io.stardog.stardao.core.field;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

import java.util.Map;

@AutoValue
public abstract class FieldData {
    @Nullable
    public abstract Field getId();
    @Nullable
    public abstract Field getCreatedAt();
    @Nullable
    public abstract Field getCreatedBy();
    @Nullable
    public abstract Field getUpdatedAt();
    @Nullable
    public abstract Field getUpdatedBy();
    public abstract Map<String,Field> getMap();

    public abstract Builder toBuilder();
    public static FieldData.Builder builder() {
        return new AutoValue_FieldData.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(Field id);
        public abstract Builder createdAt(Field field);
        public abstract Builder createdBy(Field field);
        public abstract Builder updatedAt(Field field);
        public abstract Builder updatedBy(Field field);
        public abstract Builder map(Map<String,Field> fields);
        public abstract FieldData build();
    }
}
