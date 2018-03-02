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
package com.bluenimble.platform.icli.mgm.monitors;

import java.io.File;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.CliSpec;

public class KeysMonitor {

	private long pollingInterval;
	
	public KeysMonitor (long pollingInterval) {
		if (pollingInterval <= 0) {
			pollingInterval = 5000;
		}
		this.pollingInterval = pollingInterval;
	}
	
	public KeysMonitor () {
		this (0);
	}
	
	public void start (final BlueNimble bluenimble) throws Exception {
		
        File folder = BlueNimble.keysFolder ();

        FileAlterationObserver observer = new FileAlterationObserver (folder);
        final FileAlterationMonitor monitor = new FileAlterationMonitor (pollingInterval);
        
        FileAlterationListener listener = new FileAlterationListenerAdaptor () {
            @Override
            public void onFileCreate (File file) {
            	if (!file.isFile () || !file.getName ().endsWith (CliSpec.KeysExt) || file.length () == 0) {
            		return;
            	}
                try {
					BlueNimble.loadKeys (bluenimble, file);
				} catch (Exception e) {
					bluenimble.printer ().error (e.getMessage ());
				}
            }
            @Override
            public void onFileChange (File file) {
            	if (!file.isFile () || !file.getName ().endsWith (CliSpec.KeysExt) || file.length () == 0) {
            		return;
            	}
                try {
					BlueNimble.loadKeys (bluenimble, file);
				} catch (Exception e) {
					bluenimble.printer ().error (e.getMessage ());
				}
            }
        };

        observer.addListener (listener);
        monitor.addObserver (observer);
        
        new Thread () {
        	public void run () {
                try {
					monitor.start ();
				} catch (Exception e) {
					
				}
        	}
        }.start ();
        
	}
	
}
