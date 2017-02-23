package io.stardog.stardao.auto.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.auto.value.AutoValue;
import io.stardog.stardao.auto.annotations.AutoPartial;

import java.time.LocalDate;
import java.util.Optional;

@AutoValue
@AutoPartial
public abstract class TestUser {
    public abstract String getName();
    public abstract int getAge();

    @TestAnnotation
    public abstract Optional<String> getEmail();

    public static Builder builder() {
        return new AutoValue_TestUser.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder name(String name);
        public abstract Builder age(int age);
        public abstract Builder email(String email);
        public abstract TestUser build();
    }

    @JsonIgnore
    public boolean isOk() {
        return true;
    }
}
