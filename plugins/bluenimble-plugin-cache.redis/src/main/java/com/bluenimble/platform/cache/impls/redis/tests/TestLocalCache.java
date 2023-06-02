package com.bluenimble.platform.cache.impls.redis.tests;

import java.util.Date;

import com.bluenimble.platform.cache.impls.redis.local.LocalCache;

public class TestLocalCache {
	public static void main(String[] args) {
		LocalCache cache = new LocalCache (100);
		cache.add ("key", "value", new Date ().getTime () + ( 5 * 60 * 1000));
		
		System.out.println (cache.get ("key").value);
	}
}
