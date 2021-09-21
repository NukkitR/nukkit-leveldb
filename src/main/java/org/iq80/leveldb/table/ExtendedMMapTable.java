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

import org.iq80.leveldb.util.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Comparator;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Based on the Dain's implementation {@link org.iq80.leveldb.table.MMapTable}
 * with Zlib support added
 */
public class ExtendedMMapTable extends Table {
    private MappedByteBuffer data;

    public ExtendedMMapTable(String name, FileChannel fileChannel, Comparator<Slice> comparator, boolean verifyChecksums)
            throws IOException {
        super(name, fileChannel, comparator, verifyChecksums);
        checkArgument(fileChannel.size() <= Integer.MAX_VALUE, "File must be smaller than %s bytes", Integer.MAX_VALUE);
    }

    @Override
    protected Footer init()
            throws IOException {
        long size = fileChannel.size();
        data = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
        Slice footerSlice = Slices.copiedBuffer(data, (int) size - Footer.ENCODED_LENGTH, Footer.ENCODED_LENGTH);
        return Footer.readFooter(footerSlice);
    }

    @Override
    public Callable<?> closer() {
        return new Closer(name, fileChannel, data);
    }

    private static class Closer
            implements Callable<Void> {
        private final String name;
        private final Closeable closeable;
        private final MappedByteBuffer data;

        public Closer(String name, Closeable closeable, MappedByteBuffer data) {
            this.name = name;
            this.closeable = closeable;
            this.data = data;
        }

        public Void call() {
            ByteBufferSupport.unmap(data);
            Closeables.closeQuietly(closeable);
            return null;
        }
    }

    @SuppressWarnings({"NonPrivateFieldAccessedInSynchronizedContext", "AssignmentToStaticFieldFromInstanceMethod"})
    @Override
    protected Block readBlock(BlockHandle blockHandle)
            throws IOException {
        // read block trailer
        ExtendedBlockTrailer blockTrailer = ExtendedBlockTrailer.readBlockTrailer(Slices.copiedBuffer(this.data,
                (int) blockHandle.getOffset() + blockHandle.getDataSize(),
                BlockTrailer.ENCODED_LENGTH));

        // decompress data
        Slice uncompressedData;
        ByteBuffer uncompressedBuffer = read(this.data, (int) blockHandle.getOffset(), blockHandle.getDataSize());
        switch (blockTrailer.getCompressionType()) {
            case SNAPPY: {
                synchronized (ExtendedMMapTable.class) {
                    int uncompressedLength = uncompressedLength(uncompressedBuffer);
                    if (uncompressedScratch.capacity() < uncompressedLength) {
                        uncompressedScratch = ByteBuffer.allocateDirect(uncompressedLength);
                    }
                    uncompressedScratch.clear();

                    Snappy.uncompress(uncompressedBuffer, uncompressedScratch);
                    uncompressedData = Slices.copiedBuffer(uncompressedScratch);
                }
                break;
            }
            case ZLIB: {
                synchronized (ExtendedMMapTable.class) {
                    uncompressedScratch.clear();
                    uncompressedScratch = Zlib.uncompress(uncompressedBuffer, uncompressedScratch);
                    uncompressedData = Slices.copiedBuffer(uncompressedScratch);
                    break;
                }
            }
            case ZLIB_RAW: {
                synchronized (ExtendedMMapTable.class) {
                    uncompressedScratch.clear();
                    uncompressedScratch = Zlib.uncompressRaw(uncompressedBuffer, uncompressedScratch);
                    uncompressedData = Slices.copiedBuffer(uncompressedScratch);
                    break;
                }
            }
            case NONE:
            default: {
                uncompressedData = Slices.copiedBuffer(uncompressedBuffer);
            }
        }

        return new Block(uncompressedData, comparator);
    }

    public static ByteBuffer read(MappedByteBuffer data, int offset, int length)
            throws IOException {
        int newPosition = data.position() + offset;
        return data.duplicate().order(ByteOrder.LITTLE_ENDIAN).clear().limit(newPosition + length).position(newPosition);
    }
}
