package io.stardog.stardao.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.stardog.stardao.annotations.Required;
import io.stardog.stardao.core.Update;
import io.stardog.stardao.core.field.Field;
import io.stardog.stardao.core.field.FieldData;
import io.stardog.stardao.exceptions.DataValidationException;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.groups.Default;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModelValidator {
    private final Validator validator;
    private final ObjectMapper mapper;

    public ModelValidator(Validator validator, ObjectMapper mapper) {
        this.validator = validator;
        this.mapper = mapper;
    }

    public List<ValidationError> getModelValidationErrors(Object object, Class validationGroup) {
        if (object == null) {
            return ImmutableList.of(ValidationError.of("", "object is null"));
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(object, validationGroup);
        Set<String> errorFields = new HashSet<>();
        ImmutableList.Builder<ValidationError> errors = ImmutableList.builder();
        for (ConstraintViolation<Object> v : violations) {
            String field = v.getPropertyPath().toString();
            if (!errorFields.contains(field)) {
                errors.add(ValidationError.of(field, v.getMessage()));
                errorFields.add(field);
            }
        }
        return errors.build();
    }

    /**
     * Validate a user-sourced create partial. Ensure that:
     *   - the create is only touching @Updatable fields
     *   - all non-optional @Updatable fields are being touched
     *   - the object otherwise passes validation
     * @param create    object to create
     * @param fieldData field data for model
     * @return  list of validation errors (empty if passes validation)
     */
    public List<ValidationError> getCreateValidationErrors(Object create, FieldData fieldData) {
        if (create == null) {
            return ImmutableList.of(ValidationError.of("", "create is null"));
        }

        ImmutableList.Builder<ValidationError> errors = ImmutableList.builder();
        Map<String,Object> createMap = mapper.convertValue(create, new TypeReference<Map<String,Object>>() { });
        Set<String> createFields = createMap.keySet();
        for (String fieldName : createFields) {
            Field field = fieldData.getMap().get(fieldName);
            if (field == null) {
                errors.add(ValidationError.of(fieldName, "does not exist"));
            } else if (!field.isCreatable() && !field.isUpdatable()) {
                errors.add(ValidationError.of(fieldName, "is not creatable"));
            }
        }
        for (Field field : fieldData.getMap().values()) {
            Object value = createMap.get(field.getName());
            if (!field.isOptional() && (field.isCreatable() || field.isUpdatable()) && (value == null || "".equals(value))) {
                errors.add(ValidationError.of(field.getName(), "is required"));
            }
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(create, Default.class);
        Set<String> errorFields = new HashSet<>();
        for (ConstraintViolation<?> cv : violations) {
            String field = cv.getPropertyPath().toString();
            if (createFields.contains(field) && !errorFields.contains(field)) {
                errors.add(ValidationError.of(field, cv.getMessage()));
                errorFields.add(field);
            }
        }
        return errors.build();
    }

    /**
     * Validate an user-sourced update. Ensure that:
     *   - the update is only touching @Updatable fields
     *   - the update is not unsetting any non-optional fields
     *   - the fields that are being touched all validate properly
     * @param update    update
     * @param fieldData field data for model
     * @return  list of validation errors (empty if passes validation)
     */
    public List<ValidationError> getUpdateValidationErrors(Update<?> update, FieldData fieldData) {
        if (update == null) {
            return ImmutableList.of(ValidationError.of("", "update is null"));
        }
        ImmutableList.Builder<ValidationError> errors = ImmutableList.builder();
        Set<String> updateFields = update.getUpdateFields();

        // ensure we are only touching @Updatable fields
        for (String fieldName : updateFields) {
            Field field = fieldData.getMap().get(fieldName);
            if (field == null) {
                errors.add(ValidationError.of(fieldName, "does not exist"));
            } else if (!field.isUpdatable()) {
                errors.add(ValidationError.of(fieldName, "is not updatable"));
            } else if (!field.isOptional() && update.getRemoveFields().contains(fieldName)) {
                errors.add(ValidationError.of(fieldName, "is required"));
            }
        }

        // validate the model -- but ignore fields that aren't being touched
        Set<ConstraintViolation<Object>> violations = validator.validate(update.getPartial(), Default.class);
        Set<String> errorFields = new HashSet<>();
        for (ConstraintViolation<?> cv : violations) {
            String field = cv.getPropertyPath().toString();
            if (updateFields.contains(field) && !errorFields.contains(field)) {
                errors.add(ValidationError.of(field, cv.getMessage()));
                errorFields.add(field);
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

    public boolean validateCreate(Object create, FieldData fieldData) {
        List<ValidationError> errors = getCreateValidationErrors(create, fieldData);
        if (!errors.isEmpty()) {
            throw new DataValidationException(errors);
        }
        return true;
    }

    public boolean validateUpdate(Update<?> update, FieldData fieldData) {
        List<ValidationError> errors = getUpdateValidationErrors(update, fieldData);
        if (!errors.isEmpty()) {
            throw new DataValidationException(errors);
        }
        return true;
    }
}
