package io.stardog.stardao.mongodb.codecs;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.LocalDate;

/**
 * A MongoDB codec that stores the java 8 LocalDate type as a BSON String.
 */
public class LocalDateCodec implements Codec<LocalDate> {
    @Override
    public LocalDate decode(BsonReader bsonReader, DecoderContext decoderContext) {
        return LocalDate.parse(bsonReader.readString());
    }

    @Override
    public void encode(BsonWriter bsonWriter, LocalDate localDate, EncoderContext encoderContext) {
        bsonWriter.writeString(localDate.toString());
    }

    @Override
    public Class<LocalDate> getEncoderClass() {
        return LocalDate.class;
    }
}

