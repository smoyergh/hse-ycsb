/**
 *    SPDX-License-Identifier: Apache-2.0
 *
 *    Copyright (C) 2015-2020 Micron Technology, Inc.
 *
 *    Some parts of the code herein is derived from the ycsb rocksdb binding.
 */

package site.ycsb.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.ByteBuffer;

import site.ycsb.ByteArrayByteIterator;
import site.ycsb.ByteIterator;
import site.ycsb.DB;
import site.ycsb.Status;
import site.ycsb.workloads.CoreWorkload;
import org.micron.hse.API;
import org.micron.hse.HSEEOFException;
import org.micron.hse.HSEGenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author jjacob
 */
public class HseYcsbClient extends DB {
  private static final Logger LOGGER = LoggerFactory.getLogger(HseYcsbClient.class);

  private static API hseAPI;
  private static int valBufSize = 4096; // default

  private static final AtomicInteger INIT_COUNT = new AtomicInteger(0);
  private static String defYCSBPfx = "user";
  private static int defKvsPfxLen = 7;
  private static int defCursorPfxLen = 7;
  private static int cursorPfxLen = defCursorPfxLen;

  private static String getCursorPfx(String key) {
    if (cursorPfxLen == 0 || key == null) {
      return null;
    }

    /* Use non-prefixed cursor if key length is smaller/same as pfx. length. */
    if (key.length() <= cursorPfxLen) {
      return null;
    }

    return key.substring(0, cursorPfxLen);
  }

  private static String getNextPfx(String curPfx) {
    if (cursorPfxLen == 0 || curPfx == null) {
      return null;
    }

    if (!curPfx.contains(defYCSBPfx)) {
      LOGGER.error("Default YCSB prefix string, " + defYCSBPfx +
                   ", has changed.");
      System.exit(1);
    }

    int ycsbPfxLen = defYCSBPfx.length();
    int curPfxLen = curPfx.length();

    /* Ensured by earlier checks in init() and getCursorPfx(). */
    assert(curPfxLen > ycsbPfxLen && curPfxLen == cursorPfxLen);

    // Below logic is tightly coupled with the YCSB key format: "user[0-9]+"
    Long nextPfx = new Long(0);
    try {
      nextPfx = Long.parseLong(curPfx.substring(ycsbPfxLen, curPfxLen));
      ++nextPfx;
    } catch (NumberFormatException e) {
      // [MU_REVISIT]: This restriction can be removed if we binary add 1.
      LOGGER.error("Prefix " + curPfx + " contains non-numeric chars");
      System.exit(1);
    }

    Double maxPfx = Math.pow(10, curPfxLen - ycsbPfxLen) - 1;
    if (nextPfx > maxPfx) {
      /* No more prefixes to scan. */
      return null;
    }

    return defYCSBPfx + Long.toString(nextPfx);
  }

  private String getKvdbHomeParam(Properties props) {
    return props.getProperty("hse.kvdb_home");
  }

  private String getHseConfigParam(Properties props) {
    String hseConfig = props.getProperty("hse.config");

    if (hseConfig != null) {
      hseConfig = hseConfig.replace(';', ',');
    }

    return hseConfig;
  }

  private String getCursorPfxLenParam(Properties props) {
    String pfxlen = props.getProperty("hse.cursor_pfx_len");
    return pfxlen;
  }

  /**
   * Initialize any state for this DB.
   * Called once per DB instance; there is one DB instance per client thread.
   */
  @Override
  public void init() {
    INIT_COUNT.incrementAndGet();
    synchronized (INIT_COUNT) {

      if (hseAPI == null) {
        hseAPI = new API();
        API.loadLibrary();

        Properties props = getProperties();

        String kvdbHome = getKvdbHomeParam(props);
        if(null == kvdbHome) {
          LOGGER.error("hse.kvdb_home not configured");
          System.exit(1);
        }

        String kvsName = props.getProperty(
            CoreWorkload.TABLENAME_PROPERTY,
            CoreWorkload.TABLENAME_PROPERTY_DEFAULT);
        if(null == kvsName) {
          LOGGER.error(CoreWorkload.TABLENAME_PROPERTY + " not configured");
          System.exit(1);
        }

        String kvsPath = kvdbHome + "/" + kvsName;

        String hseConfig = getHseConfigParam(props);
        if (null == hseConfig) {
          LOGGER.info("property hse.config not specified, using default configuration");
          hseConfig = "";
        } else {
          hseConfig = hseConfig.trim();
        }

        // append pfx_len param if not present
        String paramPfxLen = "kvdb.kvs.default.pfx_len=";
        if (hseConfig.isEmpty()) {
          hseConfig = paramPfxLen + defKvsPfxLen;
        } else if (!hseConfig.contains(paramPfxLen)){
          hseConfig += "," + paramPfxLen + defKvsPfxLen;
        }

        String pfxlen = getCursorPfxLenParam(props);
        if (pfxlen != null) {
          cursorPfxLen = Integer.parseInt(pfxlen);
        }

        if (cursorPfxLen > 0) {
          if (cursorPfxLen <= defYCSBPfx.length()) {
            LOGGER.error("Cursor Prefix length must be greater than the length " +
                         "of the default YCSB prefix, " + defYCSBPfx);
            System.exit(1);
          }
        }

        final double scanProportion = Double.valueOf(props.getProperty(
            CoreWorkload.SCAN_PROPORTION_PROPERTY,
            CoreWorkload.SCAN_PROPORTION_PROPERTY_DEFAULT));
        if (scanProportion > 0) {
          /* Parameter for workloads with scans, like workload E. */
          if (!hseConfig.isEmpty() && !hseConfig.endsWith(",")) {
            hseConfig += ",";
          }
          hseConfig += "kvdb.csched_vb_scatter_pct=1,kvdb.kvs.default.cn_cursor_vra=0";
        }

        String fieldCount = props.getProperty(
            CoreWorkload.FIELD_COUNT_PROPERTY,
            CoreWorkload.FIELD_COUNT_PROPERTY_DEFAULT
        );

        String fieldLength = props.getProperty(
            CoreWorkload.FIELD_LENGTH_PROPERTY,
            CoreWorkload.FIELD_LENGTH_PROPERTY_DEFAULT
        );

        int readFieldCount = Integer.parseInt(fieldCount);
        valBufSize = readFieldCount * (Integer.parseInt(fieldLength) + 20);

        LOGGER.info("hse.config=\"" + hseConfig + "\"");

        try {
          hseAPI.init(valBufSize);
          hseAPI.open((short) 1, kvdbHome, kvsPath, hseConfig);
        } catch (HSEGenException e) {
          e.printStackTrace();
          LOGGER.error("Could not open HSE with kvs path ["
              + kvsPath + "]");
          System.exit(1);
        }
      }
    }
  }

  /**
   * Cleanup any state for this DB.
   * Called once per DB instance; there is one DB instance per client thread.
   */
  @Override
  public void cleanup() {
    synchronized (INIT_COUNT) {
      // Close only for the last one
      if (INIT_COUNT.decrementAndGet() == 0) {
        try {
          hseAPI.close();
        } catch (HSEGenException e) {
          e.printStackTrace();
          LOGGER.warn("Could not close HSE");
        }
      }
    }
  }

  @Override
  public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
    try {
      byte[] keyA = key.getBytes();
      byte[] valA = hseAPI.get(keyA);
      // TODO how does API handle not found?
      deserializeValues(valA, fields, result);
      return Status.OK;
    } catch (HSEGenException e) {
      e.printStackTrace();
      return Status.ERROR;
    }
  }

  @Override
  public Status scan(String table, String startkey, int recordcount, Set<String> fields,
                     Vector<HashMap<String, ByteIterator>> result) {
    Status status = Status.OK;
    String curPfx = null;
    int curPfxLen = 0;

    // [MU_REVISIT]: Maybe cleaner to separate prefixed and non-prefixed paths.
    try {
      curPfx = getCursorPfx(startkey);
      curPfxLen = (curPfx != null) ? curPfx.length() : 0;
      hseAPI.createCursor(curPfx, curPfxLen);
    } catch (HSEGenException e) {
      e.printStackTrace();
      return Status.ERROR;
    }

    try {
      byte[] foundkey = hseAPI.seek(startkey.getBytes());
      if (foundkey == null || foundkey.length == 0) {
        LOGGER.error("Nothing found in scan for startkey=" + startkey);
        return Status.ERROR;
      }

      result.ensureCapacity(recordcount);

      int i = 0;
      int tries = 3;
      do {
        try {
          for (; i < recordcount; i++) {
            byte[] value = hseAPI.read();

            HashMap<String, ByteIterator> map = new HashMap<>();

            deserializeValues(value, fields, map);
            result.add(map);
          }
          break;
        } catch (HSEEOFException e) {
          curPfx = getNextPfx(curPfx);
          // Do nothing if this is a non-prefixed cursor or is the last prefix.
          if (curPfx == null) {
            break;
          }

          /* This loop handles the case where we are at the end of a prefix and
           * haven't scanned 'recordcount' records yet. The getNextPfx()
           * returns the next prefix in lexicographic order. If there are 3
           * successive empty prefixes, we fallback to a non-prefixed cursor
           * to avoid getting stuck in searching through a large sequence of
           * empty prefixes.
           */
          curPfxLen = curPfx.length();

          String tmpPfx = null;
          if (tries-- == 0) {
            // Fall back to a non-prefixed cursor after 3 attempts.
            tmpPfx = curPfx;
            curPfx = null;
            curPfxLen = 0;
          }

          hseAPI.destroyCursor();
          hseAPI.createCursor(curPfx, curPfxLen);

          if (tmpPfx != null) {
            foundkey = hseAPI.seek(tmpPfx.getBytes());
            if (foundkey == null || foundkey.length == 0) {
              // do nothing
              break;
            }
          }
        }
      } while(true);
    } catch (HSEGenException e) {
      e.printStackTrace();
      status = Status.ERROR;
    } finally {
      try {
        hseAPI.destroyCursor();
      } catch (HSEGenException e) {
        e.printStackTrace();
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

      hseAPI.put(keyB, valB);
    } catch (IOException | HSEGenException e) {
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

      hseAPI.put(keyB, valB);
    } catch (IOException | HSEGenException e) {
      e.printStackTrace();
      status = Status.ERROR;
    }
    return status;
  }

  @Override
  public Status delete(String table, String key) {
    Status status = Status.OK;
    try {
      hseAPI.del(key.getBytes());
    } catch (HSEGenException e) {
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
