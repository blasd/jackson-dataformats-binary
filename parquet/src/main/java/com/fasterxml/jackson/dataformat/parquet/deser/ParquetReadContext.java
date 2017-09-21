package com.fasterxml.jackson.dataformat.parquet.deser;

import com.fasterxml.jackson.dataformat.avro.deser.AvroReadContext;

/**
 * We need to use a custom context to be able to carry along
 * Object and array records.
 */
public abstract class ParquetReadContext extends AvroReadContext
{
	
	protected final ParquetReadContext _parent;
	
    /*
    /**********************************************************************
    /* Instance construction
    /**********************************************************************
     */

    public ParquetReadContext(ParquetReadContext parent, String typeId)
    {
        super(parent, typeId);
        
        this._parent = parent;
    }
}
