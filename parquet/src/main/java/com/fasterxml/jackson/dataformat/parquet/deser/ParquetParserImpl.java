package com.fasterxml.jackson.dataformat.parquet.deser;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.avro.deser.AvroParserImpl;

/**
 * Implementation class that exposes additional internal API
 * to be used as callbacks by {@link ParquetReadContext} implementations.
 */
public abstract class ParquetParserImpl
    extends AvroParserImpl
{
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected ParquetParserImpl(IOContext ctxt, int parserFeatures, int avroFeatures,
            ObjectCodec codec)
    {
        super(ctxt, parserFeatures, avroFeatures, codec);
    }

    /*
    /**********************************************************
    /* Methods for AvroReadContext impls, other
    /**********************************************************
     */

    public final void setAvroContext(ParquetReadContext ctxt) {
        _avroContext = ctxt;
    }

}
