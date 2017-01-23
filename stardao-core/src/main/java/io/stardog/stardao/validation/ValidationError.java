package io.stardog.stardao.validation;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ValidationError {
    public abstract String getField();
    public abstract String getMessage();

    public static ValidationError of(String field, String message) {
        return new AutoValue_ValidationError(field, message);
    }

    @Override
    public String toString() {
        return getField() + ": " + getMessage();
    }
}
