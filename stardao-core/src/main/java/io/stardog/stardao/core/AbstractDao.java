package io.stardog.stardao.core;

import io.stardog.stardao.core.field.FieldData;
import io.stardog.stardao.core.field.FieldScanner;
import io.stardog.stardao.exceptions.DataNotFoundException;

import java.time.Instant;

public abstract class AbstractDao<M,P,K,I> implements Dao<M,P,K> {
    private final Class<M> modelClass;
    private final Class<P> partialClass;
    private final FieldData fieldData;

    public AbstractDao(Class<M> modelClass, Class<P> partialClass) {
        this.modelClass = modelClass;
        this.partialClass = partialClass;
        this.fieldData = generateFieldData();
    }

    protected FieldData generateFieldData() {
        return new FieldScanner().scanAnnotations(modelClass);
    }

    @Override
    public Class<M> getModelClass() {
        return modelClass;
    }

    @Override
    public Class<P> getPartialClass() {
        return partialClass;
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

    public M create(P partial) {
        return create(partial, Instant.now(), null);
    }

    public M create(P partial, Instant createAt) {
        return create(partial, createAt, null);
    }

    public M create(P partial, I createBy) {
        return create(partial, Instant.now(), createBy);
    }

    public abstract M create(P partial, Instant createAt, I createBy);

    /**
     * Update an object
     * @param id    id of the object to update
     * @param update    update data
     */
    public void update(K id, Update<P> update) {
        update(id, update, Instant.now(), null);
    }

    /**
     * Update an object
     * @param id    id of the object to update
     * @param update    update data
     * @param updateAt    timestamp of the update
     * @return  state of the object prior to modification
     */
    public void update(K id, Update<P> update, Instant updateAt) {
        update(id, update, updateAt, null);
    }

    /**
     * Update an object
     * @param id    id of the object to update
     * @param update    update data
     * @param updateBy    user id of the user who performed the update
     * @return  state of the object prior to modification
     */
    public void update(K id, Update<P> update, I updateBy) {
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
    public abstract void update(K id, Update<P> update, Instant updateAt, I updateBy);

    @Override
    public M updateAndReturn(K id, Update<P> update) {
        return updateAndReturn(id, update, Instant.now(), null);
    }

    /**
     * Update an object and return the state of the object prior to modification
     * @param id    id of the object to update
     * @param update    update data
     * @param updateAt    timestamp of the update
     * @return  state of the object prior to modification
     */
    public M updateAndReturn(K id, Update<P> update, Instant updateAt) {
        return updateAndReturn(id, update, updateAt, null);
    }

    /**
     * Update an object and return the state of the object prior to modification
     * @param id    id of the object to update
     * @param update    update data
     * @param updateBy    user id of the user who performed the update
     * @return  state of the object prior to modification
     */
    public  M updateAndReturn(K id, Update<P> update, I updateBy) {
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
    public abstract M updateAndReturn(K id, Update<P> update, Instant updateAt, I updateBy);

    /**
     * Drop the backing table and re-initialize. Useful as a shortcut for tests.
     */
    public void dropAndInitTable() {
        dropTable();
        initTable();
    }
}
