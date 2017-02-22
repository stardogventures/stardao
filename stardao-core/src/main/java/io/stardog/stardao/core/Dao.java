package io.stardog.stardao.core;

import java.util.Optional;

public interface Dao<M,P,K> {
    /**
     * The POJO model class that this Dao is responsible for persisting.
     * @return  model class
     */
    public Class<M> getModelClass();

    /**
     * The POJO partial model class that this Dao is responsible for persisting.
     * @return  partial model class
     */
    public Class<P> getPartialClass();

    /**
     * Load an object by its primary key, or throw a runtime exception if the object is not found.
     * @param id    primary key value
     * @return  model object
     */
    public M load(K id);

    /**
     * Load an object by its primary key, returning an absent optional if it is not found.
     * @param id    primary key value
     * @return  optional of model object
     */
    public Optional<M> loadOpt(K id);

    /**
     * Create a new object
     * @param model object data
     * @return  the newly created object
     */
    public M create(P model);

    /**
     * Update some fields of an existing object by id
     * @param id    id of the object to update
     * @param update    update data
     */
    public void update(K id, Update<P> update);

    /**
     * Update an object and return the state of the object prior to modification
     * @param id    id of the object to update
     * @param update    update data
     * @return  state of the object prior to modification
     */
    public M updateAndReturn(K id, Update<P> update);

    /**
     * Delete an object by id
     * @param id    id of the object to delete
     */
    public void delete(K id);

    /**
     * Iterate through all objects being stored by the Dao.
     * @return  iterable cursor through all objects
     */
    public Iterable<M> iterateAll();

    /**
     * Initialize the backing table(s) for the Dao, adding/removing indexes if necessary.
     * This might lock the database, but it should never be destructive to data.
     */
    public void initTable();

    /**
     * Drop all backing table(s) for the Dao. Should be called with care!
     */
    public void dropTable();
}
