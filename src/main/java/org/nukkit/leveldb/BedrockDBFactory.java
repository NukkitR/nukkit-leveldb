package org.nukkit.leveldb;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.impl.BedrockDB;
import org.iq80.leveldb.impl.DbImpl;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;

public class BedrockDBFactory extends Iq80DBFactory {

    public static final BedrockDBFactory factory = new BedrockDBFactory();

    @Override
    public DB open(File path, org.iq80.leveldb.Options options) throws IOException {
        if (options instanceof ExtendedOptions) {
            return new BedrockDB((ExtendedOptions) options, path);
        }
        return new DbImpl(options, path);
    }
}
