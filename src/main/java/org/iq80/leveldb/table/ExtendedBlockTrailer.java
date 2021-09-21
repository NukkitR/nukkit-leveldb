/*
 * Copyright (C) 2011 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.iq80.leveldb.table;

import org.iq80.leveldb.util.Slice;
import org.iq80.leveldb.util.SliceInput;
import org.iq80.leveldb.util.SliceOutput;
import org.iq80.leveldb.util.Slices;
import org.nukkit.leveldb.ExtendedCompressionType;

import static java.util.Objects.requireNonNull;
import static org.iq80.leveldb.table.BlockTrailer.ENCODED_LENGTH;

/**
 * Based on the Dain's implementation {@link org.iq80.leveldb.table.BlockTrailer}
 * with Zlib support added
 */
public class ExtendedBlockTrailer {

    private final ExtendedCompressionType compressionType;
    private final int crc32c;

    public ExtendedBlockTrailer(ExtendedCompressionType compressionType, int crc32c) {
        requireNonNull(compressionType, "compressionType is null");
        this.compressionType = compressionType;
        this.crc32c = crc32c;
    }

    public ExtendedCompressionType getCompressionType() {
        return compressionType;
    }

    public int getCrc32c() {
        return crc32c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExtendedBlockTrailer that = (ExtendedBlockTrailer) o;
        if (crc32c != that.crc32c) {
            return false;
        }
        return compressionType == that.compressionType;
    }

    @Override
    public int hashCode() {
        int result = compressionType.hashCode();
        result = 31 * result + crc32c;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ExtendedBlockTrailer");
        sb.append("{compressionType=").append(compressionType);
        sb.append(", crc32c=0x").append(Integer.toHexString(crc32c));
        sb.append('}');
        return sb.toString();
    }

    public static ExtendedBlockTrailer readBlockTrailer(Slice slice) {
        SliceInput sliceInput = slice.input();
        ExtendedCompressionType compressionType = ExtendedCompressionType.getCompressionTypeByPersistentId(sliceInput.readUnsignedByte());
        int crc32c = sliceInput.readInt();
        return new ExtendedBlockTrailer(compressionType, crc32c);
    }

    public static Slice writeBlockTrailer(ExtendedBlockTrailer blockTrailer) {
        Slice slice = Slices.allocate(ENCODED_LENGTH);
        writeBlockTrailer(blockTrailer, slice.output());
        return slice;
    }

    public static void writeBlockTrailer(ExtendedBlockTrailer blockTrailer, SliceOutput sliceOutput) {
        sliceOutput.writeByte(blockTrailer.getCompressionType().persistentId());
        sliceOutput.writeInt(blockTrailer.getCrc32c());
    }
}
