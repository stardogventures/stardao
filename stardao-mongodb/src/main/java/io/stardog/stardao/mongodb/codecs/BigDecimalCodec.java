package io.stardog.stardao.mongodb.codecs;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Decimal128;

import java.math.BigDecimal;

/**
 * A MongoDB codec that stores the java BigDecimal type as a BSON Decimal128. Will only work in MongoDB 3.4.
 */
public class BigDecimalCodec implements Codec<BigDecimal> {
    @Override
    public BigDecimal decode(BsonReader bsonReader, DecoderContext decoderContext) {
        return bsonReader.readDecimal128().bigDecimalValue();
    }

    @Override
    public void encode(BsonWriter bsonWriter, BigDecimal bigDecimal, EncoderContext encoderContext) {
        bsonWriter.writeDecimal128(new Decimal128(bigDecimal));
    }

    @Override
    public Class<BigDecimal> getEncoderClass() {
        return BigDecimal.class;
    }
}

