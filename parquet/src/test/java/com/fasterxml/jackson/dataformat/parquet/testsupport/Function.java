package com.fasterxml.jackson.dataformat.parquet.testsupport;

import java.io.IOException;

public interface Function<T, U> {
    U apply(T input) throws IOException;
}