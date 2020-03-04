package site.ycsb.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.ByteBuffer;
import java.io.File;

import com.wiredtiger.db.Connection;
import com.wiredtiger.db.Cursor;
import com.wiredtiger.db.SearchStatus;
import com.wiredtiger.db.Session;
import com.wiredtiger.db.WiredTigerException;
import com.wiredtiger.db.wiredtiger;
import site.ycsb.ByteArrayByteIterator;
import site.ycsb.ByteIterator;
import site.ycsb.DB;
import site.ycsb.Status;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author jjacob
 */
public class WiredtigerYcsbClient extends DB {

  private static Connection wtConn = null;

  private Session wtSession = null;
  private Cursor wtCursor = null;

  private static final AtomicInteger INIT_COUNT = new AtomicInteger(0);

  /**
   * Initialize any state for this DB. Called once per DB instance; there is one
   * DB instance per client thread.
   */
  @Override
  public void init() {
    INIT_COUNT.incrementAndGet();

    synchronized (INIT_COUNT) {
      Properties props = getProperties();

      try {
        String dbPath = props.getProperty("wiredtiger.dir");

        if (null == dbPath) {
          System.out.println("Error: wiredtiger.dir not configured");
          System.exit(1);
        }

        if (wtConn == null) {
          File dbDir = new File(dbPath);

          if (!dbDir.isDirectory()) {
            boolean created = dbDir.mkdirs();

            if (!created) {
              throw new IOException("Cannot create directory " + dbDir);
            }
          }

          /*
           * Configuration settings
           */
          String defaultConfigStr = "" +
              "cache_size=16GB" +
              ",checkpoint_sync=false" +
              ",eviction=(threads_max=4)" +
              ",session_max=20000" +
              ",file_manager=(close_idle_time=100000)" +
              ",config_base=false" +
              ",log=(enabled=false)" +
              ",statistics=(fast=true)" +
              ",statistics_log=(wait=60)";

          String configStr = props.getProperty("wiredtiger.configstring");

          if (null == configStr) {
            configStr = defaultConfigStr;
          }

          // Add create flag
          configStr += ",create";

          wtConn = wiredtiger.open(dbPath, configStr);
          wtSession = wtConn.open_session(null);
          wtSession.create("table:ycsb", "key_format=u,value_format=u");
        } else {
          wtSession = wtConn.open_session(null);
        }

        wtCursor = wtSession.open_cursor("table:ycsb", null, null);

      } catch (WiredTigerException wte) {
        System.err.println("WiredTigerException: " + wte);
        System.exit(1);
      } catch (IOException ioe) {
        ioe.printStackTrace();
        System.exit(1);
      }
    }
  }

  /**
   * Cleanup any state for this DB. Called once per DB instance; there is one DB
   * instance per client thread.
   */
  @Override
  public void cleanup() {
    synchronized (INIT_COUNT) {

      wtCursor.close();
      wtSession.close(null);

      // Close only for the last one
      if (INIT_COUNT.decrementAndGet() == 0) {
        try {
          wtConn.close(null);
        } catch (WiredTigerException wte) {
          System.err.println("WiredTigerException: " + wte);
        }
      }
    }
  }

  @Override
  public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
    try {
      byte[] keyA = key.getBytes();
      wtCursor.putKeyByteArray(keyA);
      if (wtCursor.search() != 0) {
        return Status.NOT_FOUND;
      }
      byte[] valA = wtCursor.getValueByteArray();
      deserializeValues(valA, fields, result);
      return Status.OK;
    } catch (Exception e) {
      e.printStackTrace();
      return Status.ERROR;
    }
  }

  @Override
  public Status scan(String table, String startkey, int recordcount, Set<String> fields,
      Vector<HashMap<String, ByteIterator>> result) {
    int rc;
    Status status = Status.OK;

    try {
      wtCursor.putKeyByteArray(startkey.getBytes());
      SearchStatus exact = wtCursor.search_near();

      if (exact != SearchStatus.FOUND && exact != SearchStatus.LARGER) {
        System.err.println("Nothing found in scan for startkey=" + startkey);
        return Status.ERROR;
      }

      for (int i=0; i<recordcount; i++) {
        byte[] value = wtCursor.getValueByteArray();

        HashMap<String, ByteIterator> map = new HashMap<>();

        deserializeValues(value, fields, map);
        result.add(map);

        rc = wtCursor.next();
        if (rc != 0) {
          if (rc == wiredtiger.WT_NOTFOUND) {
            break;
          } else {
            System.err.println("cursor.next: " + wiredtiger.wiredtiger_strerror(rc));
            status = Status.ERROR;
          }
        }
      }
    } catch (WiredTigerException wte) {
      status = Status.ERROR;
      wte.printStackTrace();
    } finally {
      try {
        rc = wtCursor.reset();
        if (rc != 0) {
          System.err.println("cursor.reset: " + wiredtiger.wiredtiger_strerror(rc));
          status = Status.ERROR;
        }
      } catch (WiredTigerException wte) {
        wte.printStackTrace();
        status = Status.ERROR;
      }
    }

    return status;
  }

  @Override
  public Status update(String table, String key, Map<String, ByteIterator> values) {
    Status status = Status.OK;
    try {
      byte[] keyB = key.getBytes();
      byte[] valB = serializeValues(values);

      wtCursor.putKeyByteArray(keyB);
      wtCursor.putValueByteArray(valB);
      wtCursor.insert();
    } catch (Exception e) {
      e.printStackTrace();
      status = Status.ERROR;
    }
    return status;
  }

  @Override
  public Status insert(String table, String key, Map<String, ByteIterator> values) {
    Status status = Status.OK;
    try {
      byte[] keyB = key.getBytes();
      byte[] valB = serializeValues(values);

      wtCursor.putKeyByteArray(keyB);
      wtCursor.putValueByteArray(valB);
      wtCursor.insert();
    } catch (Exception e) {
      e.printStackTrace();
      status = Status.ERROR;
    }
    return status;
  }

  @Override
  public Status delete(String table, String key) {
    Status status = Status.OK;
    try {
      byte[] keyA = key.getBytes();
      wtCursor.putKeyByteArray(keyA);
      wtCursor.remove();
    } catch (Exception e) {
      e.printStackTrace();
      status = Status.ERROR;
    }
    return status;
  }

  private Map<String, ByteIterator> deserializeValues(final byte[] values, final Set<String> fields,
      final Map<String, ByteIterator> result) {
    // Borrowed from site.ycsb.db.rocksdb.RocksDBClient
    final ByteBuffer buf = ByteBuffer.allocate(4);

    int offset = 0;
    while(offset < values.length) {
      buf.put(values, offset, 4);
      buf.flip();
      final int keyLen = buf.getInt();
      buf.clear();
      offset += 4;

      final String key = new String(values, offset, keyLen);
      offset += keyLen;

      buf.put(values, offset, 4);
      buf.flip();
      final int valueLen = buf.getInt();
      buf.clear();
      offset += 4;

      if(fields == null || fields.contains(key)) {
        result.put(key, new ByteArrayByteIterator(values, offset, valueLen));
      }

      offset += valueLen;
    }

    return result;
  }

  private byte[] serializeValues(final Map<String, ByteIterator> values) throws IOException {
    // Borrowed from site.ycsb.db.rocksdb.RocksDBClient
    try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      final ByteBuffer buf = ByteBuffer.allocate(4);

      for(final Map.Entry<String, ByteIterator> value : values.entrySet()) {
        final byte[] keyBytes = value.getKey().getBytes(UTF_8);
        final byte[] valueBytes = value.getValue().toArray();

        buf.putInt(keyBytes.length);
        baos.write(buf.array());
        baos.write(keyBytes);

        buf.clear();

        buf.putInt(valueBytes.length);
        baos.write(buf.array());
        baos.write(valueBytes);

        buf.clear();
      }
      return baos.toByteArray();
    }
  }
}
