package com.bluenimble.platform.servers.broker.server.boot;

import java.io.File;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.servers.broker.server.Broker;
import com.bluenimble.platform.servers.broker.server.impls.BrokerImpl;

public class BrokerStarter {

	private static final String BNB_HOME = "BNB_HOME";
	
	public static void main (String [] args) throws Exception {
		String sHome = System.getProperty (BNB_HOME);
		if (Lang.isNullOrEmpty (sHome)) {
			sHome = Lang.BLANK;
		} else if (!sHome.endsWith (Lang.SLASH)) {
			sHome += Lang.SLASH;
		}
		
		File home = new File (sHome);
		
		Broker broker = new BrokerImpl (home);
		broker.start ();
	}
	
}
