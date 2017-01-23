package io.stardog.stardao.exceptions;

import io.stardog.stardao.validation.ValidationError;

import java.util.List;

public class DataValidationException extends DataException {
    private final List<ValidationError> errors;

    public DataValidationException(List<ValidationError> errors) {
        this.errors = errors;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        for (ValidationError error : errors) {
            sb.append(error.toString());
            sb.append("; ");
        }
        return sb.toString().substring(0, sb.toString().length()-2);
    }
}
