package io.stardog.stardao;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import io.stardog.stardao.annotations.Id;
import org.bson.types.ObjectId;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder=AutoValue_TestAddress.Builder.class)
public abstract class TestAddress {
    @Nullable
    public abstract String getCity();

    @Nullable
    public abstract String getState();

    public abstract Builder toBuilder();
    public static TestAddress.Builder builder() {
        return new AutoValue_TestAddress.Builder();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {
        public abstract Builder city(String city);
        public abstract Builder state(String state);
        public abstract TestAddress build();
    }
}