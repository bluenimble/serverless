/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluenimble.platform.pooling;

public class Poolable<T> implements AutoCloseable {

    private final T object;
    private ObjectPool<T> pool;
    private final int partition;
    private long lastAccessTs;

    public Poolable(T t, ObjectPool<T> pool, int partition) {
        this.object = t;
        this.pool = pool;
        this.partition = partition;
        this.lastAccessTs = System.currentTimeMillis();
    }

    public T getObject() {
        return object;
    }

    public ObjectPool<T> getPool() {
        return pool;
    }

    public int getPartition() {
        return partition;
    }

    public void returnObject() {
        pool.returnObject(this);
    }

    public long getLastAccessTs() {
        return lastAccessTs;
    }

    public void setLastAccessTs(long lastAccessTs) {
        this.lastAccessTs = lastAccessTs;
    }

    /**
     * This method is not idemponent, don't call it twice, which will return the object twice to the pool and cause severe problems.
     */
    @Override
    public void close() {
        this.returnObject();
    }
}
