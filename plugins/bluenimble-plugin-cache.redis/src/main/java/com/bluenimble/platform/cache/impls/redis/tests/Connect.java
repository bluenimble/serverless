package com.bluenimble.platform.cache.impls.redis.tests;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

public class Connect {

	public static void main (String [] args) {
		Jedis client = new Jedis (
			new HostAndPort ("redis-19604.c74.us-east-1-4.ec2.cloud.redislabs.com", 19604)
		);
		client.auth ("6zviBgUqSQWvmHKEW9XrnbS2eTEz27Cp");
		System.out.println ("done!");
		client.close ();
	}
	
}
