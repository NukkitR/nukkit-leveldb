package org.nukkit.leveldb;

import java.util.HashMap;
import java.util.Map;

public enum ExtendedCompressionType {
    NONE(0),
    SNAPPY(1),
    ZLIB(2),
    ZLIB_RAW(4);

    private final static ExtendedCompressionType[] VALUES = values();
    private final static Map<Integer, ExtendedCompressionType> BY_ID = new HashMap<>();

    static {
        for (ExtendedCompressionType type : VALUES) {
            BY_ID.put(type.persistentId(), type);
        }
    }

    private final int persistentId;

    public static ExtendedCompressionType getCompressionTypeByPersistentId(int persistentId) {
        ExtendedCompressionType type = BY_ID.get(persistentId);
        if (type == null) {
            throw new IllegalArgumentException("Unknown persistentId " + persistentId);
        }
        return type;
    }

    ExtendedCompressionType(int persistentId) {
        this.persistentId = persistentId;
    }

    public int persistentId() {
        return this.persistentId;
    }
}
