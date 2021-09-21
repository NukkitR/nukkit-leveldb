# Nukkit-LevelDB

Based on the of Dain's [LevelDB](https://github.com/dain/leveldb) port to Java with the support of Zlib compression
and `*.ldb` format table.

## API Usage:

Recommended Package imports:

```java
import org.iq80.leveldb.*;
import org.nukkit.leveldb.*;

import static org.nukkit.leveldb.BedrockDBFactory.*;

import java.io.*;
```

Opening and closing the database.

```java   
// Use ExtendedOptions instead of Options
ExtendedOptions options = new ExtendedOptions();
options.createIfMissing(true);
// Add support for ZLIB and ZLIB_RAW
options.extendedCompressionType(ExtendedCompressionType.ZLIB_RAW);
DB db = factory.open(new File("example"), options);
try {
    // Use the db in here....
} finally {
    // Make sure you close the db to shutdown the 
    // database and avoid resource leaks.
    db.close();
}
```

Putting, Getting, and Deleting key/values.

```java   
db.put(bytes("Tampa"), bytes("rocks"));
String value = asString(db.get(bytes("Tampa")));
db.delete(bytes("Tampa"), wo);
```

Performing Batch/Bulk/Atomic Updates.

```java   
WriteBatch batch = db.createWriteBatch();
try {
    batch.delete(bytes("Denver"));
    batch.put(bytes("Tampa"), bytes("green"));
    batch.put(bytes("London"), bytes("red"));

    db.write(batch);
} finally {
    // Make sure you close the batch to avoid resource leaks.
    batch.close();
}
```

Iterating key/values.

```java   
DBIterator iterator = db.iterator();
try {
    for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        String key = asString(iterator.peekNext().getKey());
        String value = asString(iterator.peekNext().getValue());
        System.out.println(key + " = " + value);
    }
} finally {
    // Make sure you close the iterator to avoid resource leaks.
    iterator.close();
}
```

Working against a Snapshot view of the Database.

```java   
ReadOptions ro = new ReadOptions();
ro.snapshot(db.getSnapshot());
try{
    // All read operations will now use the same 
    // consistent view of the data.
    ... = db.iterator(ro);
    ... = db.get(bytes("Tampa"), ro);

} finally {
    // Make sure you close the snapshot to avoid resource leaks.
    ro.snapshot().close();
}
```

Using a custom Comparator.

```java   
DBComparator comparator = new DBComparator() {
    public int compare(byte[] key1, byte[] key2) {
        return new String(key1).compareTo(new String(key2));
    }
    public String name() {
        return"simple";
    }
    public byte[] findShortestSeparator(byte[] start, byte[] limit) {
        return start;
    }
    public byte[] findShortSuccessor(byte[] key) {
        return key;
    }
};

ExtendedOptions options = new ExtendedOptions();
options.comparator(comparator);
DB db = factory.open(new File("example"), options);
```

Disabling Compression

```java   
ExtendedOptions options = new ExtendedOptions();
options.extendedCompressionType(ExtendedCompressionType.NONE);
DB db = factory.open(new File("example"),options);
```

Configuring the Cache

```java    
ExtendedOptions options = new ExtendedOptions();
options.cacheSize(100 * 1048576); // 100MB cache
DB db = factory.open(new File("example"), options);
```

Getting approximate sizes.

```java   
long[]sizes = db.getApproximateSizes(new Range(bytes("a"),bytes("k")), new Range(bytes("k"),bytes("z")));
System.out.println("Size: " + sizes[0] + ", " + sizes[1]);
```

Getting database status.

```java   
String stats = db.getProperty("leveldb.stats");
System.out.println(stats);
```

Getting informational log messages.

```java   
Logger logger = new Logger() {
    public void log(String message) {
        System.out.println(message);
    }
};
ExtendedOptions options = new ExtendedOptions();
options.logger(logger);
DB db = factory.open(new File("example"), options);
```

Destroying a database.

```java    
ExtendedOptions options = new ExtendedOptions();
factory.destroy(new File("example"), options);
```