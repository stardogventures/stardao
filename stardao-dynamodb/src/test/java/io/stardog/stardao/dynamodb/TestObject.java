package io.stardog.stardao.dynamodb;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class TestObject {
    private final UUID id;
    private final String name;
    private final Instant at;
    private final LocalDate date;
    private final Integer num;

    public TestObject(@JsonProperty("id") UUID id, @JsonProperty("name") String name,
                      @JsonProperty("at") Instant at, @JsonProperty("date") LocalDate date, @JsonProperty("num") Integer num) {
        this.id = id;
        this.name = name;
        this.at = at;
        this.date = date;
        this.num = num;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getAt() {
        return at;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    public LocalDate getDate() {
        return date;
    }

    public Integer getNum() {
        return num;
    }

    public String toString() {
        return "id=" + id + " name=" + name + " at=" + at + " date=" + date + " num=" + num;
    }
}