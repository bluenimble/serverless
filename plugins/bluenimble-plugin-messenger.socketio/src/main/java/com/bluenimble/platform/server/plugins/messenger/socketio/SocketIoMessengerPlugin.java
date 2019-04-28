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
package com.bluenimble.platform.server.plugins.messenger.socketio;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.messaging.Messenger;
import com.bluenimble.platform.messenger.impls.socketio.SocketIoMessenger;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.remote.Remote;
import com.bluenimble.platform.security.SslUtils;
import com.bluenimble.platform.security.SslUtils.StoreSource;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ServerFeature;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

public class SocketIoMessengerPlugin extends AbstractPlugin {

	private static final long serialVersionUID = 3203657740159783537L;
	
	private HostnameVerifier TrustAllHostVerifier = new HostnameVerifier () {
		@Override
		public boolean verify (String host, SSLSession session) {
			if (!host.equalsIgnoreCase (session.getPeerHost ())) {
                System.out.println ("Warning: URL host '" + host + "' is different than SSLSession host '" + session.getPeerHost () + "'.");
            }
			return true;
		}
	};
	
	private static final X509TrustManager TrustAllManager = new X509TrustManager () {
        public X509Certificate [] getAcceptedIssuers () { return new java.security.cert.X509Certificate[0]; }
        public void checkClientTrusted (X509Certificate[] certs, String authType) { }
        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
    };
	
	private static SSLSocketFactory TrustAllSocketFactory;
	static {
		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance ("TLS");
			sslContext.init (null, new TrustManager [] { TrustAllManager }, new java.security.SecureRandom ());
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
        TrustAllSocketFactory = sslContext.getSocketFactory ();
	}
	
	public interface Spec {
		String Http  		= "http";
		String Uri 			= "uri";
		String AuthField	= "authField";
		String ForceNew		= "forceNew";
		String Multiplex	= "multiplex";
		String Secure		= "secure";
		String Timeout		= "timeout";
		String RememberUpgrade
							= "rememberUpgrade";
		interface Reconnect	{
			String Enabled	= "enabled";
			String Attempts	= "attempts";
			String Delay	= "delay";
			String MaxDelay	= "maxDelay";
		}
		interface Pool {
			String MaxIdleConnections = "maxIdleConnections";
			String KeepAliveDuration = "keepAliveDuration";
		}
	}
	
	private String 		feature;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Feature aFeature = Messenger.class.getAnnotation (Feature.class);
		if (aFeature == null || Lang.isNullOrEmpty (aFeature.name ())) {
			return;
		}
		feature = aFeature.name ();
		
		server.addFeature (new ServerFeature () {
			private static final long serialVersionUID = 3585173809402444745L;
			@Override
			public String id () {
				return null;
			}
			@Override
			public Class<?> type () {
				return Messenger.class;
			}
			@Override
			public Object get (ApiSpace space, String name) {
				RecyclableMessenger recyclable = (RecyclableMessenger)space.getRecyclable (createKey (name));
				return new SocketIoMessenger (tracer, recyclable.http, recyclable.spec);
			}
			@Override
			public String provider () {
				return SocketIoMessengerPlugin.this.getNamespace ();
			}
			@Override
			public Plugin implementor () {
				return SocketIoMessengerPlugin.this;
			}
		});
	}
	
	@Override
	public void onEvent (Event event, Manageable target, Object... args) throws PluginRegistryException {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		ApiSpace space = (ApiSpace)target;
		
		switch (event) {
			case Create:
				createClients (space);
				break;
			case AddFeature:
				createClient (space, Json.getObject (space.getFeatures (), feature), (String)args [0], (Boolean)args [1]);
				break;
			case DeleteFeature:
				removeClient (space, (String)args [0]);
				break;
			default:
				break;
		}
	}
	
	private void createClients (ApiSpace space) throws PluginRegistryException {
		// create sessions
		JsonObject allFeatures = Json.getObject (space.getFeatures (), feature);
		if (Json.isNullOrEmpty (allFeatures)) {
			return;
		}
		
		Iterator<String> keys = allFeatures.keys ();
		while (keys.hasNext ()) {
			createClient (space, allFeatures, keys.next (), false);
		}
		
	}
	
	private void createClient (ApiSpace space, JsonObject allFeatures, String name, boolean overwrite) throws PluginRegistryException {
		
		JsonObject feature = Json.getObject (allFeatures, name);
		
		if (!this.getNamespace ().equalsIgnoreCase (Json.getString (feature, ApiSpace.Features.Provider))) {
			return;
		}
		
		String recyclableKey = createKey (name);
		if (space.containsRecyclable (recyclableKey)) {
			return;
		}
		
		JsonObject spec = Json.getObject (feature, ApiSpace.Features.Spec);
	
		if (overwrite) {
			removeClient (space, name);
		}
		
		try {
			createHttp (space, name, spec, recyclableKey, overwrite);
		} catch (Exception e) {
			throw new PluginRegistryException (e.getMessage (), e);
		}
	
		feature.set (ApiSpace.Spec.Installed, true);
	}
	
	private void createHttp (ApiSpace space, String name, JsonObject spec, String recyclableKey, boolean overwrite) throws Exception {
		
		OkHttpClient http = null;
		
		OkHttpClient.Builder builder = new OkHttpClient.Builder ();
		
		JsonObject httpSpec = Json.getObject (spec, name);

		// timeouts
		JsonObject oTimeout = Json.getObject (httpSpec, Remote.Spec.Timeout);
		
		builder.connectTimeout (Json.getInteger (oTimeout, Remote.Spec.TimeoutConnect, 10), TimeUnit.SECONDS);
		builder.writeTimeout (Json.getInteger (oTimeout, Remote.Spec.TimeoutWrite, 10), TimeUnit.SECONDS);
		builder.readTimeout (Json.getInteger (oTimeout, Remote.Spec.TimeoutRead, 10), TimeUnit.SECONDS);
		
		// pool
		JsonObject pool = Json.getObject (httpSpec, Remote.Spec.Pool);
		builder.connectionPool (
			new ConnectionPool (
				Json.getInteger (pool, Spec.Pool.MaxIdleConnections, 10),
				Json.getLong (pool, Spec.Pool.KeepAliveDuration, 60),
				TimeUnit.MINUTES
			)
		);
		
		// ssl
		JsonObject oSsl = Json.getObject (httpSpec, Remote.Spec.KeyStore);
		
		StoreSource keystore 	= loadStore (Json.getObject (oSsl, Remote.Spec.KeyStore));
		StoreSource truststore 	= loadStore (Json.getObject (oSsl, Remote.Spec.TrustStore));
		if (keystore != null || truststore != null) {
			SSLContext sslContext = SslUtils.sslContext (keystore, truststore);
			builder.sslSocketFactory (sslContext.getSocketFactory (), null);
		}
		// trust all - usefull for dev
		if (Json.getBoolean (oSsl, Remote.Spec.TrustAll, false)) {
			builder.sslSocketFactory (TrustAllSocketFactory, TrustAllManager)
					.hostnameVerifier (TrustAllHostVerifier);
		}
		
		// proxy
		JsonObject oProxy = Json.getObject (httpSpec, Remote.Spec.Proxy);
		
		if (Json.isNullOrEmpty (oProxy)) {
			http = builder.build ();
			space.addRecyclable (recyclableKey, new RecyclableMessenger (http, spec));
			return;
		}
		
		Proxy.Type type = null;
		try {
			type = Proxy.Type.valueOf (Json.getString (oProxy, Remote.Spec.ProxyType, Proxy.Type.HTTP.name ()).toUpperCase ());
		} catch (Exception ex) {
			type = Proxy.Type.HTTP;
		}
		
		String host = Json.getString 	(oProxy, Remote.Spec.ProxyHost);
		int port 	= Json.getInteger 	(oProxy, Remote.Spec.ProxyPort, 0);
		if (Lang.isNullOrEmpty (host) || port == 0) {
			http = builder.build ();
			space.addRecyclable (recyclableKey, new RecyclableMessenger (http, spec));
			return;
		}
		
		http = builder.proxy (new Proxy (type, new InetSocketAddress (host, port))).build ();
		
		if (overwrite) {
			removeClient (space, name);
		}
		
		space.addRecyclable (recyclableKey, new RecyclableMessenger (http, spec));
		
	}
	
	private void removeClient (ApiSpace space, String featureName) {
		String key = createKey (featureName);
		Recyclable recyclable = space.getRecyclable (createKey (featureName));
		if (recyclable == null) {
			return;
		}
		// remove from recyclables
		space.removeRecyclable (key);
		// recycle
		recyclable.recycle ();
	}
	
	private String createKey (String name) {
		return feature + Lang.DOT + getNamespace () + Lang.DOT + name;
	}

	class RecyclableMessenger implements Recyclable {
		private static final long serialVersionUID = 50882416501226306L;

		private OkHttpClient	http;
		private JsonObject 		spec;
		
		public RecyclableMessenger (OkHttpClient http, JsonObject spec) {
			this.http = http;
			this.spec = spec;
		}
		
		@Override
		public void finish (boolean withError) {
			// nothing
		}

		@Override
		public void recycle () {
		}
	}
	
	private StoreSource loadStore (JsonObject oStore) {
		if (Json.isNullOrEmpty (oStore) || !oStore.containsKey (Remote.Spec.StoreStream)) {
			return null;
		}
		
		final byte [] storeBytes = Base64.decodeBase64 (Json.getString (oStore, Remote.Spec.StoreStream));
		
		return new StoreSource () {
			@Override
			public String type () {
				return Json.getString (oStore, Remote.Spec.StoreType, SslUtils.PKCS12);
			}
			@Override
			public String algorithm () {
				return Json.getString (oStore, Remote.Spec.StoreAlgorithm);
			}
			@Override
			public InputStream stream () {
				return new ByteArrayInputStream (storeBytes);
			}
			@Override
			public char [] password () {
				String password = Json.getString (oStore, Remote.Spec.StorePassword);;
				if (Lang.isNullOrEmpty (password)) {
					return null;
				}
				return password.toCharArray ();
			}
			@Override
			public char [] paraphrase () {
				String paraphrase = Json.getString (oStore, Remote.Spec.KeyParaphrase);;
				if (Lang.isNullOrEmpty (paraphrase)) {
					return password ();
				}
				return paraphrase.toCharArray ();
			}
			@Override
			public void close () {
				// nothing
			}
		};
	}

}
