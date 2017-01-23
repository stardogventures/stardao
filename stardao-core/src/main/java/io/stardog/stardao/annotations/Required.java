package io.stardog.stardao.annotations;

import javax.validation.groups.Default;

/**
 * This JSR303 Validation Group interface is intended to be used for fields that are
 * "required", for create methods that need to enforce required fields.
 */
public interface Required extends Default {
}
