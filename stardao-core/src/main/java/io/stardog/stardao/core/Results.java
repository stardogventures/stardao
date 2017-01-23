package io.stardog.stardao.core;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

/**
 * An object containing some paginated results (a list of data objects), with optionally
 * a "next" field that stores the next value.
 * @param <M>   the model class being used
 * @param <N>   the value to query to return the next set of results
 */
@AutoValue
public abstract class Results<M,N> {
    public abstract List<M> getData();
    public abstract Optional<N> getNext();

    public static <M,N> Results<M,N> of(Iterable<M> data) {
        return new AutoValue_Results<M, N>(ImmutableList.copyOf(data), Optional.empty());
    }

    public static <M,N> Results<M,N> of(Iterable<M> data, N next) {
        return new AutoValue_Results<M, N>(ImmutableList.copyOf(data), Optional.of(next));
    }
}
