/**
 *    SPDX-License-Identifier: Apache-2.0
 *
 *    Copyright (C) 2015-2020 Micron Technology, Inc.
 *
 *    Some parts of the code herein is derived from the ycsb rocksdb binding.
 */

package site.ycsb.db.rocksdb;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.*;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class RocksDBOptionsFileTest {

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  private RocksDBClient instance;

  @Test
  public void loadOptionsFromFile() throws Exception {
    final String optionsPath = RocksDBClient.class.getClassLoader().getResource("testcase.ini").getPath();
    final String dbPath = tmpFolder.getRoot().getAbsolutePath();

    initDbWithOptionsFile(dbPath, optionsPath);
    checkOptions(dbPath);
  }

  private void initDbWithOptionsFile(final String dbPath, final String optionsPath) throws Exception {
    instance = new RocksDBClient();

    final Properties properties = new Properties();
    properties.setProperty(RocksDBClient.PROPERTY_ROCKSDB_DIR, dbPath);
    properties.setProperty(RocksDBClient.PROPERTY_ROCKSDB_OPTIONS_FILE, optionsPath);
    instance.setProperties(properties);

    instance.init();
    instance.cleanup();
  }

  private void checkOptions(final String dbPath) throws Exception {
    final List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>();
    final DBOptions dbOptions = new DBOptions();

    RocksDB.loadLibrary();
    OptionsUtil.loadLatestOptions(dbPath, Env.getDefault(), dbOptions, cfDescriptors);

    try {
      assertEquals(dbOptions.walSizeLimitMB(), 42);

      // the two CFs should be "default" and "usertable"
      assertEquals(cfDescriptors.size(), 2);
      assertEquals(cfDescriptors.get(0).getOptions().ttl(), 42);
      assertEquals(cfDescriptors.get(1).getOptions().ttl(), 42);
    }
    finally {
      dbOptions.close();
    }
  }
};
