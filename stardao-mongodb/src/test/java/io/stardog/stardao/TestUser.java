package io.stardog.stardao;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import io.stardog.stardao.annotations.CreatedAt;
import io.stardog.stardao.annotations.CreatedBy;
import io.stardog.stardao.annotations.Id;
import io.stardog.stardao.annotations.UpdatedAt;
import io.stardog.stardao.annotations.UpdatedBy;
import org.bson.types.ObjectId;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder=AutoValue_TestUser.Builder.class)
public abstract class TestUser {
    @Id
    @Nullable
    public abstract ObjectId getId();

    @Nullable
    public abstract String getName();

    @Nullable
    public abstract String getEmail();

    @Nullable
    public abstract LocalDate getBirthday();

    @Nullable
    public abstract Integer getCount();

    @Nullable
    public abstract Boolean getActive();

    @Nullable
    public abstract Instant getLoginAt();

    @Nullable
    public abstract List<ObjectId> getFriends();

    @Nullable
    public abstract UUID getUuid();

    @Nullable
    public abstract Type getType();
    public enum Type { NORMAL, ADMIN };

    @Nullable
    public abstract List<TestAddress> getAddresses();

    @Nullable
    public abstract Map<String,TestAddress> getAddressesByRegion();

    @Nullable
    @CreatedAt
    public abstract Instant getCreateAt();

    @Nullable
    @CreatedBy
    public abstract ObjectId getCreateId();

    @Nullable
    @UpdatedAt
    public abstract Instant getUpdateAt();

    @Nullable
    @UpdatedBy
    public abstract ObjectId getUpdateId();

    public abstract Builder toBuilder();
    public static TestUser.Builder builder() {
        return new AutoValue_TestUser.Builder();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {
        public abstract Builder id(ObjectId id);
        public abstract Builder name(String name);
        public abstract Builder email(String email);
        public abstract Builder birthday(LocalDate birthday);
        public abstract Builder count(Integer count);
        public abstract Builder active(Boolean active);
        public abstract Builder loginAt(Instant loginAt);
        public abstract Builder uuid(UUID uuid);
        public abstract Builder type(Type type);
        public abstract Builder friends(List<ObjectId> friends);
        public abstract Builder addresses(List<TestAddress> addresses);
        public abstract Builder addressesByRegion(Map<String,TestAddress> addresses);
        public abstract Builder createAt(Instant at);
        public abstract Builder createId(ObjectId id);
        public abstract Builder updateAt(Instant at);
        public abstract Builder updateId(ObjectId id);
        public abstract TestUser build();
    }
}