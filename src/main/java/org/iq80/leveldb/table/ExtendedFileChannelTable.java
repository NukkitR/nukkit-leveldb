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
import org.iq80.leveldb.util.Slices;
import org.iq80.leveldb.util.Snappy;
import org.iq80.leveldb.util.Zlib;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Comparator;

/**
 * Based on the Dain's implementation {@link org.iq80.leveldb.table.FileChannelTable}
 * with Zlib support added
 */
public class ExtendedFileChannelTable extends FileChannelTable {
    public ExtendedFileChannelTable(String name, FileChannel fileChannel, Comparator<Slice> comparator, boolean verifyChecksums) throws IOException {
        super(name, fileChannel, comparator, verifyChecksums);
    }

    @Override
    protected Block readBlock(BlockHandle blockHandle) throws IOException {
        // read block trailer
        ByteBuffer trailerData = read(blockHandle.getOffset() + blockHandle.getDataSize(), BlockTrailer.ENCODED_LENGTH);
        ExtendedBlockTrailer blockTrailer = ExtendedBlockTrailer.readBlockTrailer(Slices.copiedBuffer(trailerData));

        ByteBuffer uncompressedBuffer = read(blockHandle.getOffset(), blockHandle.getDataSize());
        Slice uncompressedData;
        switch (blockTrailer.getCompressionType()) {
            case SNAPPY: {
                synchronized (ExtendedFileChannelTable.class) {
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
                synchronized (ExtendedFileChannelTable.class) {
                    uncompressedScratch.clear();
                    uncompressedScratch = Zlib.uncompress(uncompressedBuffer, uncompressedScratch);
                    uncompressedData = Slices.copiedBuffer(uncompressedScratch);
                    break;
                }
            }
            case ZLIB_RAW: {
                synchronized (ExtendedFileChannelTable.class) {
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

    private ByteBuffer read(long offset, int length)
            throws IOException {
        ByteBuffer uncompressedBuffer = ByteBuffer.allocate(length);
        fileChannel.read(uncompressedBuffer, offset);
        if (uncompressedBuffer.hasRemaining()) {
            throw new IOException("Could not read all the data");
        }
        uncompressedBuffer.clear();
        return uncompressedBuffer;
    }
}
