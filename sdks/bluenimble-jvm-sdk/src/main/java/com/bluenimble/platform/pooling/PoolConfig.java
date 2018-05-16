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

import com.bluenimble.platform.Json;
import com.bluenimble.platform.json.JsonObject;

public class PoolConfig {
	
	interface Spec {
		String PartitionSize	= "partitionSize";
		String MinSize 			= "minSize";
		String MaxSize 			= "maxSize";
		
		String MaxWait 			= "maxWait";
		String MaxIdle 			= "maxIdle";
		
		String ScavengeInterval = "scavengeInterval";
		String ScavengeRatio 	= "scavengeRatio";
	}

    private int maxWaitMilliseconds = 5000; // when pool is full, wait at most 5 seconds, then throw an exception
    private int maxIdleMilliseconds = 300000; // objects idle for 5 minutes will be destroyed to shrink the pool size
    private int minSize = 5;
    private int maxSize = 20;
    private int partitionSize = 4;
    private int scavengeIntervalMilliseconds = 1000 * 60 * 2;
    private double scavengeRatio = 0.5; // to avoid to clean up all connections in the pool at the same time
    
    public PoolConfig () {
    }

    public PoolConfig (JsonObject config) {
        maxWaitMilliseconds = Json.getInteger (config, Spec.MaxWait, 5000); 
        maxIdleMilliseconds = Json.getInteger (config, Spec.MaxIdle, 300000);
        minSize = Json.getInteger (config, Spec.MinSize, 5);
        maxSize = Json.getInteger (config, Spec.MaxSize, 20);
        partitionSize = Json.getInteger (config, Spec.PartitionSize, 4);
        scavengeIntervalMilliseconds = Json.getInteger (config, Spec.ScavengeInterval, 1000 * 60 * 2);
        scavengeRatio = Json.getDouble (config, Spec.ScavengeRatio, 0.5);
    }

    public int getMaxWaitMilliseconds() {
        return maxWaitMilliseconds;
    }

    public PoolConfig setMaxWaitMilliseconds(int maxWaitMilliseconds) {
        this.maxWaitMilliseconds = maxWaitMilliseconds;
        return this;
    }

    public int getMinSize() {
        return minSize;
    }

    public PoolConfig setMinSize(int minSize) {
        this.minSize = minSize;
        return this;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public PoolConfig setMaxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public int getMaxIdleMilliseconds() {
        return maxIdleMilliseconds;
    }

    public PoolConfig setMaxIdleMilliseconds(int maxIdleMilliseconds) {
        this.maxIdleMilliseconds = maxIdleMilliseconds;
        return this;
    }

    public int getPartitionSize() {
        return partitionSize;
    }

    public PoolConfig setPartitionSize(int partitionSize) {
        this.partitionSize = partitionSize;
        return this;
    }

    public int getScavengeIntervalMilliseconds() {
        return scavengeIntervalMilliseconds;
    }

    public PoolConfig setScavengeIntervalMilliseconds(int scavengeIntervalMilliseconds) {
        this.scavengeIntervalMilliseconds = scavengeIntervalMilliseconds;
        return this;
    }

    public double getScavengeRatio() {
        return scavengeRatio;
    }

    public PoolConfig setScavengeRatio(double scavengeRatio) {
        if (scavengeRatio <= 0 || scavengeRatio > 1) {
            throw new IllegalArgumentException("Invalid scavenge ratio: " + scavengeRatio);
        }
        this.scavengeRatio = scavengeRatio;
        return this;
    }
}
