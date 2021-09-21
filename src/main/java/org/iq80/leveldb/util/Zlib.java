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
package org.iq80.leveldb.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Zlib {

    private static final SPI ZLIB = new SPI(false);
    private static final SPI ZLIB_RAW = new SPI(false);

    public static ByteBuffer uncompress(ByteBuffer compressed, ByteBuffer uncompressed)
            throws IOException {
        return ZLIB.uncompress(compressed, uncompressed);
    }

    public static int compress(byte[] input, int inputOffset, int length,
                               byte[] output, int outputOffset) throws IOException {
        return ZLIB.compress(input, inputOffset, length, output, outputOffset);
    }

    public static ByteBuffer uncompressRaw(ByteBuffer compressed, ByteBuffer uncompressed)
            throws IOException {
        return ZLIB_RAW.uncompress(compressed, uncompressed);
    }

    public static int compressRaw(byte[] input, int inputOffset, int length,
                                  byte[] output, int outputOffset) throws IOException {
        return ZLIB_RAW.compress(input, inputOffset, length, output, outputOffset);
    }

    private static class SPI {
        private final ThreadLocal<Deflater> deflaterThreadLocal;
        private final ThreadLocal<Inflater> inflaterThreadLocal;

        public SPI(boolean nowrap) {
            this(-1, nowrap);
        }

        public SPI(int compressionLevel, boolean nowrap) {
            this.deflaterThreadLocal = ThreadLocal.withInitial(() -> new Deflater(compressionLevel, nowrap));
            this.inflaterThreadLocal = ThreadLocal.withInitial(() -> new Inflater(nowrap));
        }

        public ByteBuffer uncompress(ByteBuffer compressed, ByteBuffer uncompressed) throws IOException {
            int readableBytes = compressed.limit() - compressed.position();
            Inflater inflater = inflaterThreadLocal.get();
            inflater.reset();

            int readerIndex = compressed.position();

            if (compressed.hasArray()) {
                inflater.setInput(compressed.array(), compressed.arrayOffset() + readerIndex, readableBytes);
            } else {
                byte[] array = new byte[readableBytes];
                compressed.get(array);
                compressed.position(readerIndex);
                inflater.setInput(array);
            }

            uncompressed = prepareDecompressBuffer(uncompressed, inflater.getRemaining() << 1);
            try {
                while (!inflater.needsInput()) {
                    byte[] outArray = uncompressed.array();
                    int writerIndex = uncompressed.position();
                    int outIndex = uncompressed.arrayOffset() + writerIndex;
                    int writableBytes = uncompressed.limit() - uncompressed.position();
                    int outputLength = inflater.inflate(outArray, outIndex, writableBytes);
                    if (outputLength > 0) {
                        uncompressed.position(writerIndex + outputLength);
                    }

                    if (inflater.finished()) {
                        break;
                    } else {
                        uncompressed = prepareDecompressBuffer(uncompressed, inflater.getRemaining() << 1);
                    }
                }

                compressed.position(readableBytes - inflater.getRemaining());
            } catch (DataFormatException e) {
                throw new IOException("decompression failure", e);
            }
            return uncompressed;
        }

        public int compress(byte[] input, int inputOffset, int length, byte[] output, int outputOffset)
                throws IOException {
            Deflater deflater = deflaterThreadLocal.get();
            deflater.reset();
            deflater.setInput(input, inputOffset, length);
            deflater.finish();

            int offset = outputOffset;
            for (; ; ) {
                int numBytes;
                do {
                    numBytes = deflater.deflate(output, offset, output.length - offset);
                    offset += numBytes;
                } while (numBytes > 0);

                if (deflater.needsInput()) {
                    break;
                } else {
                    throw new IOException("compression failure");
                }
            }
            return offset - outputOffset;
        }
    }

    private static final int CALCULATE_THRESHOLD = 4 * 1024 * 1024;// 4 MiB page
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 1;

    protected static ByteBuffer prepareDecompressBuffer(ByteBuffer buffer, int preferredSize) {
        if (buffer == null) {
            return ByteBuffer.allocateDirect(preferredSize);
        }

        int oldCapacity = buffer.capacity();
        int minNewCapacity = buffer.position() + preferredSize;

        // we need to grow the bytebuffer
        if (minNewCapacity > oldCapacity) {
            if (minNewCapacity >= MAX_CAPACITY) {
                throw new IllegalStateException("Decompression buffer has reached maximum size: " + MAX_CAPACITY);
            }
            final int threshold = CALCULATE_THRESHOLD;
            int newCapacity;

            if (minNewCapacity == threshold) {
                newCapacity = threshold;
            } else if (minNewCapacity > threshold) {
                // If over threshold, do not double but just increase by threshold.
                newCapacity = minNewCapacity + threshold;
            } else {
                // Not over threshold. Double up to 4 MiB, starting from 64.
                newCapacity = 64;
                while (newCapacity < minNewCapacity) {
                    newCapacity <<= 1;
                }
            }

            ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity);
            buffer.position(0).limit(oldCapacity);
            newBuffer.position(0).limit(oldCapacity);
            newBuffer.put(buffer).clear();
            return newBuffer;
        }
        return buffer;
    }

    public static int maxCompressedLength(int rawLength) {
        return (int) (Math.ceil(rawLength * 1.001d) + 14);
    }
}
