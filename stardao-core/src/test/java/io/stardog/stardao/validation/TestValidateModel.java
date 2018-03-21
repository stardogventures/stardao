package io.stardog.stardao.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import io.stardog.stardao.annotations.Creatable;
import io.stardog.stardao.annotations.CreatedAt;
import io.stardog.stardao.annotations.Id;
import io.stardog.stardao.annotations.Updatable;
import javax.validation.constraints.Email;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonDeserialize(builder=AutoValue_TestValidateModel.Builder.class)
public abstract class TestValidateModel {
    @Nullable
    @Id
    public abstract UUID getId();

    @Nullable
    @Email
    @Updatable
    public abstract String getEmail();

    @Nullable
    @Creatable
    public abstract String getType();

    @Nullable
    @Updatable
    public abstract Optional<String> getNickname();

    @Nullable
    public abstract Instant getLoginAt();

    @Nullable
    @CreatedAt
    public abstract Instant getCreateAt();

    public abstract TestValidateModel.Builder toBuilder();
    public static TestValidateModel.Builder builder() {
        return new AutoValue_TestValidateModel.Builder();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {
        public abstract Builder id(UUID id);
        public abstract Builder email(String email);
        public abstract Builder type(String type);
        public abstract Builder nickname(String nickname);
        public abstract Builder loginAt(Instant at);
        public abstract Builder createAt(Instant at);
        public abstract TestValidateModel build();
    }
}
