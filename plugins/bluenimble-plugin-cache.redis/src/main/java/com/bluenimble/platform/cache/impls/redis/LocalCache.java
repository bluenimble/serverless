package com.bluenimble.platform.cache.impls.redis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

/*******************************************************************************
 * Copyright [2016] [Cornelius Perkins]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Contributors:
 *    Cornelius Perkins - initial API and implementation and/or initial documentation
 *    
 * Author Cornelius Perkins (ccperkins at both github and bitbucket)
 *******************************************************************************/ 

/*************************************************************************************
 * FeCache is a very lightweight simple caching system which allows entries to be 
 * cached with different expiration times, specified in milliseconds.
 * 
 * It works like a Map, with the exception that entries are removed (pruned) when
 * they expire (or never: it is possible to insert entries which will never expire.
 * 
 * When a cache is created, it is given a cache management policy, which can specify 
 * either that expiry times are constant, or that as entries are accessed, their expiry 
 * time is updated.  For example, under the "constant" expiry policy, an entry which is 
 * given 1000 ms expiry will be prunable 1000 ms from insertion no matter how many times 
 * it's accessed.  Under "access extends" expiry policy, the same entry will only 
 * become prunable 1000 ms after its last access.
 * 
 * Pruning is performed passively - that is, entries which are due to expire are 
 * only removed (pruned) when retrieval attempts are made.
 * 
 * 
 * Use is simple:
 *     1) Decide what kind of entries you wish to cache, and how you'll retrieve 
 *          them.  In the example below, the entries are called TestMessage, and 
 *          they're identified and retrieved by unique String identifiers, but 
 *          anything with valid hash/equals methods will do for a key.
 *     2) create the cache
 * Example:
 * 		LocalCache<String, TestMessage> cache = new LocalCache<> (ExpiryPolicy.EXTENDED_BY_ACCESS));
 * 		
 *     3) Then, insert entries, Given a TestMessage entry called msg and an expiry time in 
 *         milliseconds, store an entry:
 * 		TestMessage msg = new TestMessage (someKey, blah, ...);
 * 		cache.store(someKey, msg, new SimpleCacheExpiry(expiry));		
 * 
 *     4) Retrieval is also simple:
 * 		TestMessage m = cache.retrieve(someKey);
 * 		
 * 		If the entry has expired and been pruned, the return will be null.
 */

public class LocalCache<K, T> {

	/** Whether to reset the pruning time on each access, or hold it constant from the time an object was cached. */
	public enum ExpiryPolicy {
		EXTENDED_BY_ACCESS
		, FIXED 
	}
	/** Holds the entries in the cache, along with the insertion time */
	/*package*/ class Holder {

		/** The value being held. */
		public final T value;

		/** The time (in milliseconds) the value was put into the holder. */
		public final long creationTime;

		/** The expiry rule. */
		public final CacheExpiry expiry;

		/** NON-FINAL: time in ms the holder's entry was last accessed. */
		public volatile AtomicLong lastAccessTime;

		public Holder(T value, CacheExpiry expiry) {
			this (value, System.currentTimeMillis(), expiry);
		}
		public Holder(T value, long creationTime, CacheExpiry expiry) {
			this(value, creationTime, expiry, creationTime);
		}
		public Holder(T value, long creationTime, CacheExpiry expiry, long lastAccessTime) {
			super();
			this.value = value;
			this.creationTime = creationTime;
			this.expiry = expiry;
			this.lastAccessTime = new AtomicLong (lastAccessTime);
		}
		public void touch() {
			lastAccessTime.set(System.currentTimeMillis());
		}

		public boolean isExpired(ExpiryPolicy expiryPolicy) {
			if (expiryPolicy.equals(ExpiryPolicy.FIXED))
				return expiry.isExpired(creationTime);
			else
				return expiry.isExpired(lastAccessTime.get());
		}
	}


	/*package*/ static class CacheExpiry { 
		/** Length of time the entry can stay valid. */
		private final Long lifeLengthInMs;

		/** Constructs an entry with the given length of life. */
		public CacheExpiry(Long lifeLengthInMs) {
			super();
			this.lifeLengthInMs = lifeLengthInMs;
		}

		/** Special instance which specifies that an entry should never expire due to time. */
		public final static CacheExpiry NEVER_EXPIRE = new CacheExpiry (null);

		@Override
		public String toString() {
			return "lifeLengthInMs=" + lifeLengthInMs ;
		}
		/** Returns the length of life. */
		public long getLifeLengthInMs() {
			return lifeLengthInMs;
		}

		/**
		 * Determines whether a cache entry has expired.
		 * @param lifeStartTime - the start point in time (can be last access, or 
		 *    time of cache insertion, we don't care - it's just a start point to us.
		 * @return true if the entry has expired, or false.
		 */
		public boolean isExpired(Long lifeStartTime) {
			if (this.equals(NEVER_EXPIRE)) 
				return false;
			else if ((lifeStartTime == null) || Double.isNaN(lifeStartTime)) 
				throw new IllegalArgumentException ("Invalid life start time (" + lifeStartTime + ")");

			boolean isExpired= (System.currentTimeMillis() > (lifeStartTime + getLifeLengthInMs()));
			return isExpired;
		}	

	}




	/** Controls access to the cache. */
	private final Object MAP_UPDATE_LOCK = new Object();

	/** Implements the cache storage. */
	private final Map<K,Holder> map = new HashMap<>();

	/** Whether entry expiry time is fixed or whether access extends the expiry. */
	public final ExpiryPolicy expiryPolicy;

	/** Constructs an instance with the given set of cache management policies. */
	public LocalCache (ExpiryPolicy expiryPolicy) {
		this.expiryPolicy = expiryPolicy;
	}

	/** Constructs an instance with a default cache expiry policy. */
	public LocalCache () {
		this(ExpiryPolicy.EXTENDED_BY_ACCESS);
	}

	/** Stores a value with the given expiry specification. */
	void store(K key, T value, CacheExpiry expiry) {
		prune();
		Holder holder = new Holder(value, expiry);
		map.put(key, holder);
	}

	/**
	 * Attempts to retrieve a value cached with the give key. 
	 * @param key
	 * @return the cached value, or null if not found.
	 */
	T retrieve (K key) {
		prune();
		Holder holder;
		synchronized (MAP_UPDATE_LOCK) {
			holder = map.get(key);
		}
		if (holder == null) 
			return null;
		else {
			holder.touch();
			return holder.value;
		}
	}

	/**
	 * Removes a single cached value, returning the value that was there.
	 * 
	 * @param key
	 * @return
	 */
	T explicitRemove (K key) {
		Holder holder;
		synchronized (MAP_UPDATE_LOCK) {
			holder = map.get(key);
			if (holder == null)
				return null;
			else {
				map.remove(key);
				return holder.value;
			}
		}
	}

	/**
	 * Allows caller to request that the pruning happen now.  
	 * Note that this may or may not be a no-op going forward, 
	 * especially in the active pruning mode.
	 */
	public void requestPrune() {
		prune();
	}

	/**
	 * Goes through the cache, removing entries which have expired.
	 */
	private void prune() {
		synchronized (MAP_UPDATE_LOCK) {
			for(Iterator<Entry<K, Holder>> it = map.entrySet().iterator(); it.hasNext(); ) {
				Entry<K, Holder> entry = it.next();
				Holder holder = entry.getValue();
				if (holder.isExpired(expiryPolicy)) {
					it.remove();
				}
			}
		}
	}
	
}
