/**
 *
 */
package site.ycsb.db;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

import com.sleepycat.db.Database;
import com.sleepycat.db.DatabaseConfig;
import com.sleepycat.db.DatabaseEntry;
import com.sleepycat.db.DatabaseException;
import com.sleepycat.db.DatabaseType;
import com.sleepycat.db.LockMode;
import site.ycsb.ByteArrayByteIterator;
import site.ycsb.ByteIterator;
import site.ycsb.DB;
import site.ycsb.Status;


/**
 * @author jjacob
 */
public class BdbYcsbClient extends DB {

  private static final int PACK_BUF_CAP = 8192;

  private static Database db = null;

  private static Object szLock = new Object();
  private static boolean gotSize = false;
  private static long keySize = -1;
  private static long valSize = -1;

  private static final AtomicInteger INIT_COUNT = new AtomicInteger(0);

  private void getSizes(byte[] key, byte[] val) {
    if (!gotSize) {
      synchronized (szLock) {
        keySize = key.length;
        valSize = val.length;
        gotSize = true;
      }
    }
  }

  /**
   * Initialize any state for this DB. Called once per DB instance; there is
   * one DB instance per client thread.
   */
  @Override
  public void init() {

    INIT_COUNT.incrementAndGet();

    synchronized (INIT_COUNT) {
      if (db != null) {
        return;
      }
      // load library
      if (System.getProperty("os.name").startsWith("Windows")) {
        // windows
        System.loadLibrary("db_java-6.1-win");
      } else {
        // linux
        System.loadLibrary("db_java-6.1");
      }

      DatabaseConfig dbConfig = new DatabaseConfig();
      dbConfig.setErrorStream(System.err);
      dbConfig.setErrorPrefix("YCSC-BDB");
      dbConfig.setType(DatabaseType.BTREE);
      dbConfig.setAllowCreate(true);
      dbConfig.setPageSize(4096); // 4k

      String dbPath = this.getProperties().getProperty("bdb_path");
      if (null == dbPath) {
        System.out.println("Error: bdb path not configured");
        System.exit(1);
      }

      try {
        db = new Database(dbPath, null, dbConfig);
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        System.out.println("Error: Could not open BDB database at " + dbPath);
        System.exit(1);
      } catch (DatabaseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        System.out.println("Error: Could not open BDB database at " + dbPath);
        System.exit(1);
      }
    }
  }

  /**
   * Cleanup any state for this DB. Called once per DB instance; there is one
   * DB instance per client thread.
   */
  @Override
  public void cleanup() {
    if (INIT_COUNT.decrementAndGet() == 0) {
      if (null != db) {
        try {
          db.close();
        } catch (DatabaseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          System.out.println("Warning: Could not close BDB database");
        }
      }

      // Output the key and Val sizes
      System.out.println("[INFO], KEYSIZE=" + keySize + ", VALUESIZE=" + valSize);
    }
  }

  @Override
  public Status read(String table, String key, Set<String> fields, HashMap<String, ByteIterator> result) {
    Status status = Status.OK;

    DatabaseEntry rKey = new DatabaseEntry(key.getBytes());
    DatabaseEntry rVal = new DatabaseEntry();
    try {
      db.get(null, rKey, rVal, LockMode.DEFAULT);
    } catch (DatabaseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      status = Status.ERROR;
    }
    this.unpackValues(rVal.getData(), result);
    return status;
  }

  @Override
  public Status scan(String table, String startkey, int recordcount, Set<String> fields,
                     Vector<HashMap<String, ByteIterator>> result) {
    // Not implemented, no API available yet.
    return Status.NOT_IMPLEMENTED;
  }

  @Override
  public Status update(String table, String key, HashMap<String, ByteIterator> values) {
    Status status = Status.OK;

    byte[] keyB = key.getBytes();
    byte[] valB = this.packValues(values);

    getSizes(keyB, valB);

    DatabaseEntry wKey = new DatabaseEntry(keyB);
    DatabaseEntry wVal = new DatabaseEntry(valB);

    try {
      db.put(null, wKey, wVal);
    } catch (DatabaseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      status = Status.ERROR;
    }

    return status;
  }

  @Override
  public Status insert(String table, String key, HashMap<String, ByteIterator> values) {
    Status status = Status.OK;

    byte[] keyB = key.getBytes();
    byte[] valB = this.packValues(values);

    getSizes(keyB, valB);

    DatabaseEntry wKey = new DatabaseEntry(keyB);
    DatabaseEntry wVal = new DatabaseEntry(valB);

    try {
      db.put(null, wKey, wVal);
    } catch (DatabaseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      status = Status.ERROR;
    }

    return status;
  }

  @Override
  public Status delete(String table, String key) {
    Status status = Status.OK;

    DatabaseEntry wKey = new DatabaseEntry(key.getBytes());
    try {
      db.delete(null, wKey);
    } catch (DatabaseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      status = Status.ERROR;
    }

    return status;
  }

  public byte[] packValues(HashMap<String, ByteIterator> values) {

    ByteBuffer buf = ByteBuffer.allocate(PACK_BUF_CAP);
    for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
      String key = entry.getKey();
      ByteIterator btI = entry.getValue();

      buf.putInt(key.length());
      buf.put(key.getBytes());

      byte[] ba = btI.toArray();
      buf.putLong(ba.length);
      buf.put(ba);

    }

    int pos = buf.position();
    byte[] out = new byte[pos];
    buf.rewind();
    buf.get(out);
    return out;
  }

  public void unpackValues(byte[] bA, HashMap<String, ByteIterator> hm) {

    ByteBuffer buf = ByteBuffer.wrap(bA);
    HashMap<String, ByteIterator> pHM = new HashMap<String, ByteIterator>();

    while (buf.hasRemaining()) {

      int keyLen = buf.getInt();
      byte[] bk = new byte[keyLen];
      buf.get(bk);

      long valLen = buf.getLong();
      byte[] bv = new byte[(int) valLen];
      buf.get(bv);

      pHM.put(new String(bk), new ByteArrayByteIterator(bv));
    }

    hm.putAll(pHM);
  }

}
