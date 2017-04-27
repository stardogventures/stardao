package io.stardog.stardao.core;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class TestDao extends AbstractDao<TestModel,TestModel,UUID,UUID> {
    public TestDao(Class<TestModel> modelClass) {
        super(modelClass,modelClass);
    }

    @Override
    public TestModel load(UUID id) {
        return null;
    }

    @Override
    public Optional<TestModel> loadOpt(UUID id) {
        return null;
    }

    @Override
    public Optional<TestModel> loadOpt(UUID id, Iterable<String> fields) {
        return null;
    }

    @Override
    public TestModel create(TestModel model, Instant createAt, UUID createBy) {
        return null;
    }

    @Override
    public void update(UUID id, Update<TestModel> update, Instant updateAt, UUID updateBy) {

    }

    @Override
    public TestModel updateAndReturn(UUID id, Update<TestModel> update, Instant updateAt, UUID updateBy) {
        return null;
    }

    @Override
    public void delete(UUID id) {

    }

    @Override
    public Iterable<TestModel> iterateAll() {
        return null;
    }

    @Override
    public void initTable() {

    }

    @Override
    public void dropTable() {

    }
}
