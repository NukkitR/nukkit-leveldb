package org.nukkit.leveldb;

import io.netty.buffer.ByteBufUtil;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

import java.io.File;
import java.io.IOException;

import static org.nukkit.leveldb.BedrockDBFactory.bytes;

public class DBTest {

    public static void main(String... args) throws IOException {
        ExtendedOptions options = new ExtendedOptions();
        options.createIfMissing(false);
        options.extendedCompressionType(ExtendedCompressionType.ZLIB_RAW);
        options.cacheSize(40 * 1024 * 1024);
        options.writeBufferSize(4 * 1024 * 1024);

        try (DB db = BedrockDBFactory.factory.open(new File("D:\\Development\\Nukkit\\ldb-test\\db"), options)) {
            try (DBIterator iterator = db.iterator()) {
                for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                    byte[] key = iterator.peekNext().getKey();
                    byte[] value = iterator.peekNext().getValue();
                    System.out.println("Key:\t" + ByteBufUtil.hexDump(key));
                    System.out.println("Value:\t" + ByteBufUtil.hexDump(value));
                }
            }
            db.put(bytes("test-key"), bytes("test-value"));
        }
    }
}
