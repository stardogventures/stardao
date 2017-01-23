package io.stardog.stardao.validation;

import com.google.common.collect.ImmutableList;
import io.stardog.stardao.annotations.Required;
import io.stardog.stardao.core.Update;
import io.stardog.stardao.exceptions.DataValidationException;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.groups.Default;
import java.util.List;
import java.util.Set;

public class ModelValidator {
    private final Validator validator;

    public ModelValidator(Validator validator) {
        this.validator = validator;
    }

    public List<ValidationError> getModelValidationErrors(Object object, Class validationGroup) {
        if (object == null) {
            return ImmutableList.of(ValidationError.of("", "object is null"));
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(object, validationGroup);
        return violations.stream()
               .map((v) -> ValidationError.of(v.getPropertyPath().toString(), v.getMessage()))
               .collect(ImmutableList.toImmutableList());
    }

    public List<ValidationError> getUpdateValidationErrors(Update<?> update, Set<String> updatableFields) {
        if (update == null) {
            return ImmutableList.of(ValidationError.of("", "update is null"));
        }
        ImmutableList.Builder<ValidationError> errors = ImmutableList.builder();
        Set<String> updateFields = update.getUpdateFields();

        // ensure we are only touching @Updatable fields
        for (String field : updateFields) {
            if (!updatableFields.contains(field)) {
                errors.add(ValidationError.of(field, "is not updatable"));
            }
        }

        // validate the model in "required" mode -- but ignore fields that aren't being touched
        Set<ConstraintViolation<Object>> violations = validator.validate(update.getSetObject(), Required.class);
        for (ConstraintViolation<?> cv : violations) {
            String field = cv.getPropertyPath().toString();
            if (updateFields.contains(field)) {
                errors.add(ValidationError.of(field, cv.getMessage()));
            }
        }

        return errors.build();
    }

    public boolean validateModel(Object model) {
        List<ValidationError> errors = getModelValidationErrors(model, Default.class);
        if (!errors.isEmpty()) {
            throw new DataValidationException(errors);
        }
        return true;
    }

    public boolean validateRequired(Object model) {
        List<ValidationError> errors = getModelValidationErrors(model, Required.class);
        if (!errors.isEmpty()) {
            throw new DataValidationException(errors);
        }
        return true;
    }

    public boolean validateUpdate(Update<?> update, Set<String> updatableFields) {
        List<ValidationError> errors = getUpdateValidationErrors(update, updatableFields);
        if (!errors.isEmpty()) {
            throw new DataValidationException(errors);
        }
        return true;
    }
}
