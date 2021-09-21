package org.nukkit.leveldb;

import org.iq80.leveldb.impl.Filename;

import java.io.File;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class ExtendedFilename {

    /**
     * Return the name of the *.sst table with the specified number.
     */
    public static String sstTableFileName(long number) {
        return makeFileName(number, "sst");
    }

    /**
     * Return the name of the *.ldb table with the specified number.
     */
    public static String ldbTableFileName(long number) {
        return makeFileName(number, "ldb");
    }

    /**
     * If filename is a leveldb file, store the type of the file in *type.
     * The number encoded in the filename is stored in *number.  If the
     * filename was successfully parsed, returns true.  Else return false.
     */
    public static Filename.FileInfo parseFileName(File file) {
        // Owned filenames have the form:
        //    dbname/CURRENT
        //    dbname/LOCK
        //    dbname/LOG
        //    dbname/LOG.old
        //    dbname/MANIFEST-[0-9]+
        //    dbname/[0-9]+.(log|sst|dbtmp|ldb)
        String fileName = file.getName();
        if (fileName.endsWith(".ldb")) {
            long fileNumber = Long.parseLong(removeSuffix(fileName, ".ldb"));
            return new Filename.FileInfo(Filename.FileType.TABLE, fileNumber);
        }
        return Filename.parseFileName(file);
    }

    private static String makeFileName(long number, String suffix) {
        checkArgument(number >= 0, "number is negative");
        requireNonNull(suffix, "suffix is null");
        return String.format("%06d.%s", number, suffix);
    }

    private static String removeSuffix(String value, String suffix) {
        return value.substring(0, value.length() - suffix.length());
    }
}
