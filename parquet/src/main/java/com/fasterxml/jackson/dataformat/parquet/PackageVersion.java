package com.fasterxml.jackson.dataformat.parquet;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.core.util.VersionUtil;

// TODO HARDCODED TEMPORARILY
public final class PackageVersion implements Versioned {
    public final static Version VERSION = VersionUtil.parseVersion(
        "2.9.0-SNAPSHOT", "com.fasterxml.jackson.core", "jackson-parquet");

    @Override
    public Version version() {
        return VERSION;
    }
}
