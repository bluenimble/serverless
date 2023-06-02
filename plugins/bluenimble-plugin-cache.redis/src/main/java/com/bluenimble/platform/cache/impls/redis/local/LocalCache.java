package com.bluenimble.platform.cache.impls.redis.local;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class LocalCache {
	
	public class Entry {
		public long 	expiry;
		public Object	value;
		Entry (Object value, long expiry) {
			this.expiry = expiry;
			this.value 	= value;
		}
	}
	
	private Map<String, Entry> 	cache;
	private int 				totalCapacity = 100;
	private Timer 				timer;
	
	public LocalCache (int totalCapacity) {
		this.totalCapacity = totalCapacity;
		cache = new ConcurrentHashMap<String, Entry> (totalCapacity < 100 ? totalCapacity : (int)(totalCapacity / 10), 0.75f);
		this.timer = new Timer ();
	    timer.scheduleAtFixedRate (new TimerTask () {
			@Override
			public void run () {
				if (LocalCache.this.cache.isEmpty ()) {
					return;
				}
				Iterator<Map.Entry<String, Entry>> iterator = LocalCache.this.cache.entrySet().iterator ();
				while (iterator.hasNext ()) {
					Map.Entry<String, Entry> entry = iterator.next ();
					if (new Date ().getTime () >= entry.getValue ().expiry) {
						iterator.remove ();
					}
				}
			}
	    }, 0, 5 * 60 * 1000);
	}
	
    public Entry get (String key) {
    	Entry entry = cache.get (key);
    	if (entry == null) {
    		return null;
    	}
    	if (entry.expiry == 0) {
    		return entry;
    	}
    	if (new Date ().getTime () >= entry.expiry) {
    		cache.remove (key);
    		return null;
    	}
        return entry;
    }

    public void add (String key, Object value, long ttl) {
        if (key == null || value == null) {
            return;
        }
        if (cache.size () >= totalCapacity) {
        	return;
        }
        cache.put (key, new Entry ( value, ttl > 0 ? new Date ().getTime () + ttl : 0));
    }

    public void remove (String key) {
        if (key == null) {
            return;
        }
        cache.remove (key);
    }
    
    public void destroy () {
    	if (timer == null) {
    		return;
    	}
    	timer.cancel ();
    	timer.purge ();
    }

}
