package com.fasterxml.jackson.dataformat.parquet.apacheimpl;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.avro.AvroParser;
import com.fasterxml.jackson.dataformat.avro.apacheimpl.ApacheAvroFactory;
import com.fasterxml.jackson.dataformat.avro.apacheimpl.ApacheAvroParserImpl;
import com.fasterxml.jackson.dataformat.parquet.ParquetFactory;

/**
 * Alternative {@link ParquetFactory} implementation that uses
 * codecs from Apache Parquet library.
 */
public class ApacheParquetFactory extends ParquetFactory
{
    private static final long serialVersionUID = 1L;

    public ApacheParquetFactory() {
        this(null);
    }

    public ApacheParquetFactory(ObjectCodec oc) {
        super(oc);
    }

    protected ApacheParquetFactory(ParquetFactory src, ObjectCodec oc) {
        super(src, oc);
    }

    /*
    /**********************************************************
    /* Factory method overrides
    /**********************************************************
     */
    
    @Override
    public ParquetFactory copy()
    {
        _checkInvalidCopy(ApacheAvroFactory.class);
        return new ApacheParquetFactory(this, null);
    }

    @Override
    protected AvroParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        return new ApacheAvroParserImpl(ctxt, _parserFeatures, _avroParserFeatures,
                _objectCodec, in);
    }

    @Override
    protected AvroParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        return new ApacheAvroParserImpl(ctxt, _parserFeatures, _avroParserFeatures,
                _objectCodec, data, offset, len);
    }
}
