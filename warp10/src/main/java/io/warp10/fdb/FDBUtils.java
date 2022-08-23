//
//   Copyright 2022  SenX S.A.S.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package io.warp10.fdb;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.FDBException;
import com.apple.foundationdb.Transaction;

import io.warp10.WarpConfig;
import io.warp10.continuum.Configuration;
import io.warp10.continuum.sensision.SensisionConstants;
import io.warp10.continuum.store.Constants;
import io.warp10.json.JsonUtils;
import io.warp10.sensision.Sensision;

public class FDBUtils {

  public static final String KEY_ID = "id";
  public static final String KEY_PREFIX = "prefix";

  public static final int MAX_VALUE_SIZE = 100000;
  public static final long MAX_TXN_SIZE = 10000000;

  private static final String DEFAULT_FDB_API_VERSION = Integer.toString(710);

  static {
    int version = Integer.parseInt(WarpConfig.getProperty(Configuration.FDB_API_VERSION, DEFAULT_FDB_API_VERSION));

    FDB.selectAPIVersion(version);
  }

  public static FDB getFDB() {
    return FDB.instance();
  }

  public static FDBContext getContext(String clusterFile, String tenant) {
    return new FDBContext(clusterFile, tenant);
  }

  public static byte[] getNextKey(byte[] key) {
    return getNextKey(key, 0, key.length);
  }

  public static byte[] getNextKey(byte[] key, int offset, int len) {
    if (0 != len) {
      // Return a byte array which is 'one bit after' prefix
      byte[] next = null;

      if ((byte) 0xff != key[offset + len - 1]) {
        next = Arrays.copyOfRange(key, offset, offset + len);
        next[next.length - 1] = (byte) (((((int) next[next.length - 1]) & 0xff) + 1) & 0xff);
      } else {
        next = Arrays.copyOfRange(key, offset, offset + len + 1);
        next[next.length - 1] = (byte) 0x00;
      }
      return next;
    } else {
      return key;
    }
  }

  public static byte[] addPrefix(byte[] prefix, byte[] key) {
    if (null == prefix) {
      return key;
    } else {
      byte[] pkey = Arrays.copyOf(prefix, prefix.length + key.length);
      System.arraycopy(key, 0, pkey, prefix.length, key.length);
      return pkey;
    }
  }

  public static void errorMetrics(String component, Throwable t) {
    if (!(t instanceof FDBException)) {
      return;
    }
    FDBException fdbe = (FDBException) t;
    Map<String,String> labels = new LinkedHashMap<String,String>();
    labels.put(SensisionConstants.SENSISION_LABEL_COMPONENT, component);
    labels.put(SensisionConstants.SENSISION_LABEL_CODE, Integer.toString(fdbe.getCode()));
    Sensision.update(SensisionConstants.CLASS_FDB_ERRORS, labels, 1);
  }

  public static Map<String,Object> getTenantInfo(FDBContext context, String tenant) {
    Database db = context.getDatabase();
    try {
      return getTenantInfo(db, tenant);
    } finally {
      try { db.close(); } catch (Throwable t) {}
    }
  }

  public static Map<String,Object> getTenantInfo(Database db, String tenant) {
    Transaction txn = db.createTransaction();
    txn.options().setRawAccess();
    txn.options().setAccessSystemKeys();

    Map<String,Object> map = new LinkedHashMap<String,Object>();

    // Retrieve the system key for the given tenant
    try {
      byte[] tenantMap = txn.get(getTenantSystemKey(tenant)).get();
      if (null != tenantMap) {
        Object json = JsonUtils.jsonToObject(new String(tenantMap, StandardCharsets.UTF_8));

        map.put(KEY_ID, ((Number) ((Map) json).get(KEY_ID)).longValue());
        map.put(KEY_PREFIX, ((String) ((Map) json).get(KEY_PREFIX)).getBytes(StandardCharsets.ISO_8859_1));
      }
    } catch (Throwable t) {
    } finally {
      try { txn.close(); } catch (Throwable t) {}
    }

    return map;
  }

  private static byte[] getTenantSystemKey(String tenant) {
    // Tenant key is '\xff\xff/management/tenant_map/TENANT'
    byte[] systemKey = ("xx/management/tenant_map/" + tenant).getBytes(StandardCharsets.UTF_8);
    systemKey[0] = (byte) 0xff;
    systemKey[1] = (byte) 0xff;
    return systemKey;
  }

  public static long getEstimatedRangeSizeBytes(FDBContext context, byte[] from, byte[] to) {
    Database db = context.getDatabase();
    Transaction txn = db.createTransaction();
    long size = 0L;

    try {
      txn.options().setRawAccess();
      size = txn.getEstimatedRangeSizeBytes(from, to).get().longValue();
    } catch (Throwable t) {
    } finally {
      try { txn.close(); } catch (Throwable t) {}
      try { db.close(); } catch (Throwable t) {}
    }

    return size;
  }
}
