package com.fasterxml.jackson.dataformat.avro.deser;

import java.util.*;

import org.apache.avro.Schema;
import org.apache.avro.util.internal.JacksonUtils;

import com.fasterxml.jackson.dataformat.avro.deser.ArrayReader;
import com.fasterxml.jackson.dataformat.avro.deser.AvroFieldDefaulters;
import com.fasterxml.jackson.dataformat.avro.deser.AvroFieldReader;
import com.fasterxml.jackson.dataformat.avro.deser.AvroReaderFactory;
import com.fasterxml.jackson.dataformat.avro.deser.AvroStructureReader;
import com.fasterxml.jackson.dataformat.avro.deser.MapReader;
import com.fasterxml.jackson.dataformat.avro.deser.RecordReader;
import com.fasterxml.jackson.dataformat.avro.deser.ScalarDecoder;
import com.fasterxml.jackson.dataformat.avro.deser.ScalarDecoderWrapper;
import com.fasterxml.jackson.dataformat.avro.deser.UnionReader;
import com.fasterxml.jackson.dataformat.avro.deser.ScalarDecoder.*;
import com.fasterxml.jackson.dataformat.avro.schema.AvroSchemaHelper;

/**
 * Helper class used for constructing a hierarchic reader for given
 * (reader-) schema.
 */
public abstract class ParquetReaderFactory extends AvroReaderFactory
{
    /**
     * To resolve cyclic types, need to keep track of resolved named
     * types.
     */
    protected final TreeMap<String, AvroStructureReader> _knownReaders
        = new TreeMap<String, AvroStructureReader>();

    /*
    /**********************************************************************
    /* Public API: root methods to create reader
    /**********************************************************************
     */

    public static AvroStructureReader createFor(Schema schema) {
        return new NonResolving().createReader(schema);
    }

    public static AvroStructureReader createFor(Schema writerSchema,
            Schema readerSchema) {
        return new Resolving().createReader(writerSchema, readerSchema);
    }

    /*
    /**********************************************************************
    /* Implementations
    /**********************************************************************
     */

    /**
     * Implementation used when no schema-resolution is needed, when we are using
     * same schema for reading as was used for writing.
     *
     */
    private static class NonResolving extends AvroReaderFactory
    {
        protected NonResolving() { }
    }

    /**
     * Implementation used when schema-resolution is needed, when we are using
     * different schema for reading ("reader schema") than was used for
     * writing encoded data ("writer schema")
     */
    private static class Resolving extends AvroReaderFactory
    {
        protected Resolving() { }

        /**
         * Method for creating a reader instance for specified type.
         */
        public AvroStructureReader createReader(Schema writerSchema, Schema readerSchema)
        {
            // NOTE: it is assumed writer-schema has been modified with aliases so
            //   that the names are same, so we could use either name:
            AvroStructureReader reader = _knownReaders.get(AvroSchemaHelper.getFullName(readerSchema));
            if (reader != null) {
                return reader;
            }
            // but the type to decode must be writer-schema indicated one (but
            // also same as or promotable to reader-schema one)
            switch (writerSchema.getType()) {
            case ARRAY:
                return createArrayReader(writerSchema, readerSchema);
            case MAP: 
                return createMapReader(writerSchema, readerSchema);
            case RECORD:
                return createRecordReader(writerSchema, readerSchema);
            case UNION:
                return createUnionReader(writerSchema, readerSchema);
            default:
                // for other types, we need wrappers
                return new ScalarDecoderWrapper(createScalarValueDecoder(writerSchema));
            }
        }

        protected AvroStructureReader createArrayReader(Schema writerSchema, Schema readerSchema)
        {
            readerSchema = _verifyMatchingStructure(readerSchema, writerSchema);
            Schema writerElementType = writerSchema.getElementType();
            ScalarDecoder scalar = createScalarValueDecoder(writerElementType);
            String typeId = AvroSchemaHelper.getTypeId(readerSchema);
            String elementTypeId = readerSchema.getProp(AvroSchemaHelper.AVRO_SCHEMA_PROP_ELEMENT_CLASS);

            if (scalar != null) {
                return ArrayReader.construct(scalar, typeId, elementTypeId);
            }
            return ArrayReader.construct(createReader(writerElementType, readerSchema.getElementType()), typeId, elementTypeId);
        }

        protected AvroStructureReader createMapReader(Schema writerSchema, Schema readerSchema)
        {
            readerSchema = _verifyMatchingStructure(readerSchema, writerSchema);
            Schema writerElementType = writerSchema.getValueType();
            ScalarDecoder dec = createScalarValueDecoder(writerElementType);
            String typeId = AvroSchemaHelper.getTypeId(readerSchema);
            String keyTypeId = readerSchema.getProp(AvroSchemaHelper.AVRO_SCHEMA_PROP_KEY_CLASS);
            if (dec != null) {
                String valueTypeId = readerSchema.getValueType().getProp(AvroSchemaHelper.AVRO_SCHEMA_PROP_CLASS);
                return MapReader.construct(dec, typeId, keyTypeId, valueTypeId);
            }
            return MapReader.construct(createReader(writerElementType, readerSchema.getElementType()), typeId, keyTypeId);
        }

        protected AvroStructureReader createRecordReader(Schema writerSchema, Schema readerSchema)
        {
            readerSchema = _verifyMatchingStructure(readerSchema, writerSchema);

            // Ok, this gets bit more complicated: need to iterate over writer schema
            // (since that will be the order fields are decoded in!), but also
            // keep track of which reader fields are being produced.

            final List<Schema.Field> writerFields = writerSchema.getFields();

            // but first: find fields that only exist in reader-side and need defaults,
            // and remove those from 
            Map<String,Schema.Field> readerFields = new HashMap<String,Schema.Field>();
            List<Schema.Field> defaultFields = new ArrayList<Schema.Field>();
            {
                Set<String> writerNames = new HashSet<String>();
                for (Schema.Field f : writerFields) {
                    writerNames.add(f.name());
                }
                for (Schema.Field f : readerSchema.getFields()) {
                    String name = f.name();
                    if (writerNames.contains(name)) {
                        readerFields.put(name, f);
                    } else {
                        defaultFields.add(f);
                    }
                }
            }
            
            // note: despite changes, we will always have known number of field entities,
            // ones from writer schema -- some may skip, but there's entry there
            AvroFieldReader[] fieldReaders = new AvroFieldReader[writerFields.size()
                                                                   + defaultFields.size()];
            RecordReader reader = new RecordReader.Resolving(fieldReaders, AvroSchemaHelper.getTypeId(readerSchema));

            // as per earlier, names should be the same
            _knownReaders.put(AvroSchemaHelper.getFullName(readerSchema), reader);
            int i = 0;
            for (Schema.Field writerField : writerFields) {
                Schema.Field readerField = readerFields.get(writerField.name());
                // need a skipper:
                fieldReaders[i++] = (readerField == null)
                        ? createFieldSkipper(writerField.name(),
                                writerField.schema())
                        : createFieldReader(readerField.name(),
                                writerField.schema(), readerField.schema());
            }
            
            // Any defaults to consider?
            if (!defaultFields.isEmpty()) {
                for (Schema.Field defaultField : defaultFields) {
                    AvroFieldReader fr =
                        AvroFieldDefaulters.createDefaulter(defaultField.name(), JacksonUtils.toJsonNode(defaultField.defaultVal()));
                    if (fr == null) {
                        throw new IllegalArgumentException("Unsupported default type: "+defaultField.schema().getType());
                    }
                    fieldReaders[i++] = fr;
                }
            }
            return reader;
        }

        protected AvroStructureReader createUnionReader(Schema writerSchema, Schema readerSchema)
        {
            final List<Schema> types = writerSchema.getTypes();
            AvroStructureReader[] typeReaders = new AvroStructureReader[types.size()];
            int i = 0;
            
            // !!! TODO: actual resolution !!!
            
            for (Schema type : types) {
                typeReaders[i++] = createReader(type);
            }
            return new UnionReader(typeReaders);
        }

        protected AvroFieldReader createFieldReader(String name,
                Schema writerSchema, Schema readerSchema)
        {
            ScalarDecoder scalar = createScalarValueDecoder(writerSchema);
            if (scalar != null) {
                return scalar.asFieldReader(name, false);
            }
            return AvroFieldReader.construct(name,
                    createReader(writerSchema, readerSchema));
        }

        protected AvroFieldReader createFieldSkipper(String name,
                Schema writerSchema)
        {
            ScalarDecoder scalar = createScalarValueDecoder(writerSchema);
            if (scalar != null) {
                return scalar.asFieldReader(name, true);
            }
            return AvroFieldReader.constructSkipper(name,
                    createReader(writerSchema));
        }

        /**
         * Helper method that verifies that the given reader schema is compatible
         * with specified writer schema type: either directly (same type), or
         * via latter being a union with compatible type. In latter case, type
         * (schema) within union that matches writer schema is returned instead
         * of containing union
         *
         * @return Reader schema that matches expected writer schema
         */
        private Schema _verifyMatchingStructure(Schema readerSchema, Schema writerSchema)
        {
            final Schema.Type expectedType = writerSchema.getType();
            Schema.Type actualType = readerSchema.getType();
            // Simple rules: if structures are the same (both arrays, both maps, both records),
            // fine, without yet verifying element types
            if (actualType == expectedType) {
                return readerSchema;
            }
            // Or, similarly, find the first structure of same type within union.

            // !!! 07-Feb-2017, tatu: Quite possibly we should do recursive match check here,
            //    in case there are multiple alternatives of same structured type.
            //    But since that is quite non-trivial let's wait for a good example of actual
            //    usage before tackling that.
            
            if (actualType == Schema.Type.UNION) {
                for (Schema sch : readerSchema.getTypes()) {
                    if (sch.getType() == expectedType) {
                        return sch;
                    }
                }
                throw new IllegalStateException(String.format(
                        "Mismatch between types: expected %s (name '%s'), encountered %s of %d types without match",
                        expectedType, writerSchema.getName(), actualType, readerSchema.getTypes().size()));
            }
            throw new IllegalStateException(String.format(
                    "Mismatch between types: expected %s (name '%s'), encountered %s",
                    expectedType, writerSchema.getName(), actualType));
        }
    }
}
