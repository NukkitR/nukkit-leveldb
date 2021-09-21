package org.nukkit.leveldb;

import org.iq80.leveldb.CompressionType;

public class ExtendedOptions extends org.iq80.leveldb.Options {

    private ExtendedCompressionType compressionType = ExtendedCompressionType.ZLIB_RAW;

    static void checkArgNotNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException("The " + name + " argument cannot be null");
        }
    }

    @Override
    public CompressionType compressionType() {
        throw new UnsupportedOperationException("Use extendedCompressionType() instead");
    }

    @Override
    public ExtendedOptions compressionType(CompressionType compressionType) {
        throw new UnsupportedOperationException("Use extendedCompressionType(ExtendedCompressionType) instead");
    }

    public ExtendedCompressionType extendedCompressionType() {
        return compressionType;
    }

    public ExtendedOptions extendedCompressionType(ExtendedCompressionType compressionType) {
        checkArgNotNull(compressionType, "compressionType");
        this.compressionType = compressionType;
        return this;
    }
}
