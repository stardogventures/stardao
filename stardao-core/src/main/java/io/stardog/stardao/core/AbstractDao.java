package io.stardog.stardao.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.stardog.stardao.annotations.CreatedAt;
import io.stardog.stardao.annotations.CreatedBy;
import io.stardog.stardao.annotations.FieldName;
import io.stardog.stardao.annotations.Id;
import io.stardog.stardao.annotations.Updatable;
import io.stardog.stardao.annotations.UpdatedAt;
import io.stardog.stardao.annotations.UpdatedBy;
import io.stardog.stardao.exceptions.DataNotFoundException;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public abstract class AbstractDao<M,K,I> implements Dao<M,K> {
    private final Class<M> modelClass;
    private final String idField;
    private final String createdByField;
    private final String createdAtField;
    private final String updatedByField;
    private final String updatedAtField;
    private final Set<String> updatableFields;
    private final Map<String,String> renameFields;

    public AbstractDao(Class<M> modelClass) {
        this.modelClass = modelClass;

        ImmutableSet.Builder<String> updatableFields = ImmutableSet.builder();
        ImmutableMap.Builder<String,String> renameFields = ImmutableMap.builder();
        String idField = null;
        String createdByField = null;
        String createdAtField = null;
        String updatedByField = null;
        String updatedAtField = null;

        for (Method method : modelClass.getDeclaredMethods()) {
            String field = toFieldName(method);
            if (field != null) {
                if (method.isAnnotationPresent(Updatable.class)) {
                    updatableFields.add(field);
                }
                if (method.isAnnotationPresent(Id.class)) {
                    if (idField != null) {
                        throw new IllegalStateException("Multiple @Id annotations present on " + idField + " and " + field);
                    }
                    idField = field;
                }
                if (method.isAnnotationPresent(CreatedAt.class)) {
                    if (createdAtField != null) {
                        throw new IllegalStateException("Multiple @CreatedAt annotations present on " + createdByField + " and " + field);
                    }
                    createdAtField = field;
                }
                if (method.isAnnotationPresent(CreatedBy.class)) {
                    if (createdByField != null) {
                        throw new IllegalStateException("Multiple @CreatedBy annotations present on " + createdByField + " and " + field);
                    }
                    createdByField = field;
                }
                if (method.isAnnotationPresent(UpdatedAt.class)) {
                    if (updatedAtField != null) {
                        throw new IllegalStateException("Multiple @UpdatedAt annotations present on " + createdByField + " and " + field);
                    }
                    updatedAtField = field;
                }
                if (method.isAnnotationPresent(UpdatedBy.class)) {
                    if (updatedByField != null) {
                        throw new IllegalStateException("Multiple @UpdatedBy annotations present on " + createdByField + " and " + field);
                    }
                    updatedByField = field;
                }
                if (method.isAnnotationPresent(FieldName.class)) {
                    String rename = method.getAnnotation(FieldName.class).value();
                    renameFields.put(field, rename);
                }
            }
        }
        this.idField = idField;
        this.createdByField = createdByField;
        this.createdAtField = createdAtField;
        this.updatedByField = updatedByField;
        this.updatedAtField = updatedAtField;
        this.updatableFields = updatableFields.build();
        this.renameFields = renameFields.build();
    }

    protected String toFieldName(Method method) {
        if (method.getName().startsWith("get")) {
            String field = method.getName().substring(3);
            return field.substring(0, 1).toLowerCase() + field.substring(1);
        } else {
            return null;
        }
    }

    @Override
    public Class<M> getModelClass() {
        return modelClass;
    }

    public String getIdField() {
        return idField;
    }

    public String getCreatedByField() {
        return createdByField;
    }

    public String getCreatedAtField() {
        return createdAtField;
    }

    public String getUpdatedByField() {
        return updatedByField;
    }

    public String getUpdatedAtField() {
        return updatedAtField;
    }

    public Set<String> getUpdatableFields() {
        return updatableFields;
    }

    public Map<String, String> getRenameFields() {
        return renameFields;
    }

    public String getDisplayModelName() {
        return getModelClass().getSimpleName();
    }

    @Override
    public M load(K id) {
        return loadOpt(id)
                .orElseThrow(() -> new DataNotFoundException(getDisplayModelName() + " not found: " + id));
    }

    public M create(M model) {
        return create(model, Instant.now(), null);
    }

    public M create(M model, Instant createAt) {
        return create(model, createAt, null);
    }

    public M create(M model, I createBy) {
        return create(model, Instant.now(), createBy);
    }

    public abstract M create(M model, Instant createAt, I createBy);

    /**
     * Update an object
     * @param id    id of the object to update
     * @param update    update data
     */
    public void update(K id, Update<M> update) {
        update(id, update, Instant.now(), null);
    }

    /**
     * Update an object
     * @param id    id of the object to update
     * @param update    update data
     * @param updateAt    timestamp of the update
     * @return  state of the object prior to modification
     */
    public void update(K id, Update<M> update, Instant updateAt) {
        update(id, update, updateAt, null);
    }

    /**
     * Update an object
     * @param id    id of the object to update
     * @param update    update data
     * @param updateBy    user id of the user who performed the update
     * @return  state of the object prior to modification
     */
    public void update(K id, Update<M> update, I updateBy) {
        update(id, update, Instant.now(), updateBy);
    }

    /**
     * Update an object
     * @param id    id of the object to update
     * @param update    update data
     * @param updateAt    timestamp of the update
     * @param updateBy    user id of the user who performed the update
     * @return  state of the object prior to modification
     */
    public abstract void update(K id, Update<M> update, Instant updateAt, I updateBy);

    @Override
    public M updateAndReturn(K id, Update<M> update) {
        return updateAndReturn(id, update, Instant.now(), null);
    }

    /**
     * Update an object and return the state of the object prior to modification
     * @param id    id of the object to update
     * @param update    update data
     * @param updateAt    timestamp of the update
     * @return  state of the object prior to modification
     */
    public M updateAndReturn(K id, Update<M> update, Instant updateAt) {
        return updateAndReturn(id, update, updateAt, null);
    }

    /**
     * Update an object and return the state of the object prior to modification
     * @param id    id of the object to update
     * @param update    update data
     * @param updateBy    user id of the user who performed the update
     * @return  state of the object prior to modification
     */
    public  M updateAndReturn(K id, Update<M> update, I updateBy) {
        return updateAndReturn(id, update, Instant.now(), updateBy);
    }

    /**
     * Update an object and return the state of the object prior to modification
     * @param id    id of the object to update
     * @param update    update data
     * @param updateAt    timestamp of the update
     * @param updateBy    user id of the user who performed the update
     * @return  state of the object prior to modification
     */
    public abstract M updateAndReturn(K id, Update<M> update, Instant updateAt, I updateBy);

    /**
     * Drop the backing table and re-initialize. Useful as a shortcut for tests.
     */
    public void dropAndInitTable() {
        dropTable();
        initTable();
    }
}
