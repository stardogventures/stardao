package io.stardog.stardao.core;

import io.stardog.stardao.core.field.FieldData;
import io.stardog.stardao.core.field.FieldScanner;
import io.stardog.stardao.exceptions.DataNotFoundException;

import java.time.Instant;

public abstract class AbstractDao<M,K,I> implements Dao<M,K> {
    private final Class<M> modelClass;
    private final FieldData fieldData;

    public AbstractDao(Class<M> modelClass) {
        this.modelClass = modelClass;
        this.fieldData = generateFieldData();
    }

    protected FieldData generateFieldData() {
        return new FieldScanner().scanAnnotations(modelClass);
    }

    @Override
    public Class<M> getModelClass() {
        return modelClass;
    }

    public FieldData getFieldData() {
        return fieldData;
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
