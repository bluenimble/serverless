package com.bluenimble.platform.cache.impls.redis.tests;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

public class Connect {

	public static void main (String [] args) {
		Jedis client = new Jedis (
			new HostAndPort ("", 19604)
		);
		client.auth ("");
		System.out.println ("done!");
		client.close ();
	}
	
}
