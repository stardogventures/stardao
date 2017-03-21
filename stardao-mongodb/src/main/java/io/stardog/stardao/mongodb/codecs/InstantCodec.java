package io.stardog.stardao.mongodb.codecs;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A MongoDB codec that stores the java 8 Instant type as a BSON Date.
 */
public class InstantCodec implements Codec<Instant> {
    @Override
    public Instant decode(BsonReader bsonReader, DecoderContext decoderContext) {
        return Instant.ofEpochMilli(bsonReader.readDateTime());
    }

    @Override
    public void encode(BsonWriter bsonWriter, Instant instant, EncoderContext encoderContext) {
        bsonWriter.writeDateTime(instant.toEpochMilli());
    }

    @Override
    public Class<Instant> getEncoderClass() {
        return Instant.class;
    }
}

