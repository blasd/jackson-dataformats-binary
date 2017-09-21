package com.fasterxml.jackson.dataformat.parquet;

import org.junit.Assert;

import com.fasterxml.jackson.dataformat.avro.AvroSchema;

import java.io.IOException;
import java.util.Random;

public class FixedFieldTest extends AvroTestBase
{
    private static final String FIXED_SCHEMA_JSON = "{\n"
            + "    \"type\": \"record\",\n"
            + "    \"name\": \"WithFixedField\",\n"
            + "    \"fields\": [\n"
            + "        {\"name\": \"fixedField\", \"type\": {\"type\": \"fixed\", \"name\": \"FixedFieldBytes\", \"size\": 4}}\n"
            + "    ]\n"
            + "}";

    public void testFixedField() throws IOException {
        ParquetMapper mapper = getMapper();
        AvroSchema schema = mapper.schemaFrom(FIXED_SCHEMA_JSON);

        WithFixedField in = new WithFixedField();
        byte[] bytes = {0, 1, 2, (byte) new Random().nextInt(256)};
        in.fixedField = bytes;
        byte[] serialized = mapper.writer(schema).writeValueAsBytes(in);
        WithFixedField deser = mapper.readerFor(WithFixedField.class).with(schema).readValue(serialized);
        Assert.assertArrayEquals(bytes, deser.fixedField);
    }

    static class WithFixedField {
        public byte[] fixedField;
    }
}
