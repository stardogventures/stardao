package io.stardog.stardao.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import io.stardog.stardao.annotations.CreatedAt;
import io.stardog.stardao.annotations.CreatedBy;
import io.stardog.stardao.annotations.Id;
import io.stardog.stardao.annotations.Required;
import io.stardog.stardao.annotations.Updatable;
import io.stardog.stardao.annotations.UpdatedAt;
import io.stardog.stardao.annotations.UpdatedBy;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

import javax.annotation.Nullable;
import javax.validation.constraints.Pattern;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder=AutoValue_TestModel.Builder.class)
public abstract class TestModel {
    @Nullable
    @Id
    public abstract UUID getId();

    @Nullable
    @Updatable
    @NotEmpty(groups = Required.class)
    public abstract String getName();

    @Nullable
    @Updatable
    @Email
    @Pattern(regexp=".+@.+\\..+", message = "invalid email")
    public abstract String getEmail();

    @Nullable
    @Updatable
    @NotEmpty(groups = Required.class)
    public abstract String getCountry();

    @Nullable
    @Updatable
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    public abstract LocalDate getBirthday();

    @Nullable
    @Updatable
    public abstract Boolean getActive();

    @Nullable
    @CreatedBy
    public abstract UUID getCreateId();

    @Nullable
    @CreatedAt
    public abstract Instant getCreateAt();

    @Nullable
    @UpdatedBy
    public abstract UUID getUpdateId();

    @Nullable
    @UpdatedAt
    public abstract Instant getUpdateAt();

    @Nullable
    public abstract Instant getLoginAt();

    public Instant getLoginAt(long millis) {
        return Instant.ofEpochMilli(millis);
    }

    public abstract Builder toBuilder();
    public static TestModel.Builder builder() {
        return new AutoValue_TestModel.Builder();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {
        public abstract Builder id(UUID id);
        public abstract Builder name(String name);
        public abstract Builder email(String email);
        public abstract Builder country(String state);
        public abstract Builder birthday(LocalDate state);
        public abstract Builder active(Boolean active);
        public abstract Builder createAt(Instant at);
        public abstract Builder createId(UUID id);
        public abstract Builder updateAt(Instant at);
        public abstract Builder updateId(UUID id);
        public abstract Builder loginAt(Instant at);
        public abstract TestModel build();
    }
}
