/**
 *    Copyright 2009-2020 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache.decorators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * The 2nd level cache transactional buffer.
 * <p>
 * This class holds all cache entries that are to be added to the 2nd level cache during a Session.
 * Entries are sent to the cache when commit is called or discarded if the Session is rolled back.
 * Blocking cache support has been added. Therefore any get() that returns a cache miss
 * will be followed by a put() so any lock associated with the key can be released.
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class TransactionalCache implements Cache {

  private static final Log log = LogFactory.getLog(TransactionalCache.class);

  //真正的缓存对象，和上⾯的Map<Cache, TransactionalCache>中的Cache是同⼀个
  private final Cache delegate;
  private boolean clearOnCommit;
  // 在事务被提交前，所有从数据库中查询的结果将缓存在此集合中
  private final Map<Object, Object> entriesToAddOnCommit;
  // 在事务被提交前，当缓存未命中时，CacheKey 将会被存储在此集合中
  private final Set<Object> entriesMissedInCache;

  public TransactionalCache(Cache delegate) {
    this.delegate = delegate;
    this.clearOnCommit = false;
    this.entriesToAddOnCommit = new HashMap<>();
    this.entriesMissedInCache = new HashSet<>();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  @Override
  public Object getObject(Object key) {
    // 查询的时候是直接从delegate中去查询的，也就是从真正的缓存对象中查询
    // issue #116
    Object object = delegate.getObject(key);
    if (object == null) {
      // 缓存未命中，则将 key 存⼊到 entriesMissedInCache 中
      entriesMissedInCache.add(key);
    }
    // issue #146
    if (clearOnCommit) {
      return null;
    } else {
      return object;
    }
  }

  @Override
  public void putObject(Object key, Object object) {
    // 将键值对存⼊到 entriesToAddOnCommit 这个Map中中，⽽⾮真实的缓存对象 delegate 中
    entriesToAddOnCommit.put(key, object);
  }

  @Override
  public Object removeObject(Object key) {
    return null;
  }

  @Override
  public void clear() {
    clearOnCommit = true;
    // 清空 entriesToAddOnCommit，但不清空 delegate 缓存
    entriesToAddOnCommit.clear();
  }

  public void commit() {
    // 根据 clearOnCommit 的值决定是否清空 delegate
    if (clearOnCommit) {
      delegate.clear();
    }
    // 刷新未缓存的结果到 delegate 缓存中
    flushPendingEntries();
    // 重置 entriesToAddOnCommit 和 entriesMissedInCache
    reset();
  }

  public void rollback() {
    unlockMissedEntries();
    reset();
  }

  private void reset() {
    clearOnCommit = false;
    // 直接清空所有要写入的缓存
    entriesToAddOnCommit.clear();
    entriesMissedInCache.clear();
  }

  private void flushPendingEntries() {
    for (Map.Entry<Object, Object> entry : entriesToAddOnCommit.entrySet()) {
      // 写入最底层的缓存中
      // 将 entriesToAddOnCommit 中的内容转存到 delegate 中
      // 在这⾥真正的将entriesToAddOnCommit的对象逐个添加到delegate中，只有这时，⼆级缓存才真正 的⽣效
      delegate.putObject(entry.getKey(), entry.getValue());
    }
    for (Object entry : entriesMissedInCache) {
      if (!entriesToAddOnCommit.containsKey(entry)) {
        // 存⼊空值
        delegate.putObject(entry, null);
      }
    }
  }

  private void unlockMissedEntries() {
    for (Object entry : entriesMissedInCache) {
      try {
        // 调⽤ removeObject 进⾏解锁
        delegate.removeObject(entry);
      } catch (Exception e) {
        log.warn("Unexpected exception while notifying a rollback to the cache adapter. "
            + "Consider upgrading your cache adapter to the latest version. Cause: " + e);
      }
    }
  }

}
