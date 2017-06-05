package io.stardog.stardao.core;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import io.swagger.annotations.ApiModelProperty;

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
    @ApiModelProperty(value = "list of results returned", required = true)
    public abstract List<M> getData();

    @ApiModelProperty(value = "identifier to return the next item in the sequence")
    public abstract Optional<N> getNext();

    public static <M,N> Results<M,N> of(Iterable<M> data) {
        return new AutoValue_Results<M, N>(ImmutableList.copyOf(data), Optional.empty());
    }

    public static <M,N> Results<M,N> of(Iterable<M> data, N next) {
        return new AutoValue_Results<M, N>(ImmutableList.copyOf(data), Optional.ofNullable(next));
    }
}
