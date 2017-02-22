package io.stardog.stardao.validation;

import io.stardog.stardao.core.AbstractDao;
import io.stardog.stardao.core.Update;

import javax.validation.Validation;

public class DefaultValidator {
    private static final ModelValidator VALIDATOR = new ModelValidator(Validation.buildDefaultValidatorFactory().getValidator());

    public static boolean validateModel(Object model) {
        return VALIDATOR.validateModel(model);
    }

    public static boolean validateRequired(Object model) {
        return VALIDATOR.validateRequired(model);
    }

    public static <T> boolean validateUpdate(Update<T> update, AbstractDao<?,T,?,?> dao) {
        return VALIDATOR.validateUpdate(update, dao.getFieldData());
    }
}
