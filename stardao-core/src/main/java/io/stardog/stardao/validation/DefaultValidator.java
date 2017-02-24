package io.stardog.stardao.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.stardog.stardao.core.AbstractDao;
import io.stardog.stardao.core.Update;

import javax.validation.Validation;

public class DefaultValidator {
    private static final ModelValidator VALIDATOR = new ModelValidator(Validation.buildDefaultValidatorFactory().getValidator(),
            new ObjectMapper()
                    .registerModule(new Jdk8Module()));

    public static boolean validateModel(Object model) {
        return VALIDATOR.validateModel(model);
    }

    public static <T> boolean validateCreate(T create, AbstractDao<?,T,?,?> dao) {
        return VALIDATOR.validateCreate(create, dao.getFieldData());
    }

    @Deprecated
    public static boolean validateRequired(Object model) {
        return VALIDATOR.validateRequired(model);
    }

    public static <T> boolean validateUpdate(Update<T> update, AbstractDao<?,T,?,?> dao) {
        return VALIDATOR.validateUpdate(update, dao.getFieldData());
    }
}
