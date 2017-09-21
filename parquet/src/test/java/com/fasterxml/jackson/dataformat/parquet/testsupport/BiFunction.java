package com.fasterxml.jackson.dataformat.parquet.testsupport;

import java.io.IOException;

public interface BiFunction<T, U, V> {
    V apply(T first, U second) throws IOException;
}