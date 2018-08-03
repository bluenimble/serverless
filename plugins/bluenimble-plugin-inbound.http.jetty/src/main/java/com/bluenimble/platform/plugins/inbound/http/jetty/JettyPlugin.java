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
package com.bluenimble.platform.plugins.inbound.http.jetty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequestBodyReader;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.CodeExecutor;
import com.bluenimble.platform.api.impls.readers.JsonApiRequestBodyReader;
import com.bluenimble.platform.api.impls.readers.StreamApiRequestBodyReader;
import com.bluenimble.platform.api.impls.readers.TextApiRequestBodyReader;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.plugins.inbound.http.impl.HttpApiRequest;
import com.bluenimble.platform.plugins.inbound.http.impl.HttpApiResponse;
import com.bluenimble.platform.plugins.inbound.http.readers.YamlApiRequestBodyReader;
import com.bluenimble.platform.reflect.BeanUtils;
import com.bluenimble.platform.server.ApiServer;

public class JettyPlugin extends AbstractPlugin {
	
	private static final long serialVersionUID = 4642997488038621776L;
	
	private static final String 	FavIcon 		= "favicon.ico";
	private static final String 	FavIconPath 	= Lang.SLASH + FavIcon;
	private static 	 	 byte [] 	FavIconContent 	= null;

	interface Pool {
		String Min 				= "min";
		String Max 				= "max";
		String IdleTimeout 		= "idleTimeout";
		String Capacity 		= "capacity";
	}

	interface Ssl {
		String Port 				= "port";
		// idleTimeout in seconds
		String IdleTimeout 			= "idleTimeout";
		String Keystore 			= "keystore";
		String Password 			= "password";
		String StorePassword 		= "storePassword";
		String StoreType 			= "storeType";
		interface ciphers 			{
			String Include = "include";
			String Exclude = "exclude";
		}
	}
	
	interface Cors {
        String Origins 			= "origins";
        String Methods 			= "methods";
        String Headers 			= "headers";
	        String Allow 		= "allow]";
	        String Expose 		= "expose";
	}
	
	private static final JsonArray DefaultExposedHeaders = new JsonArray ();
	static {
		//Authorization,Cache-Control,Content-Type,DNT,If-Modified-Since,Keep-Alive,Origin,User-Agent,X-Requested-With,BNB-Execution-Time,BNB-Node-Id,BNB-Node-Type
		DefaultExposedHeaders.add (ApiHeaders.ContentType);
		DefaultExposedHeaders.add (ApiHeaders.ContentLength);
		DefaultExposedHeaders.add (ApiHeaders.ContentDisposition);
		DefaultExposedHeaders.add (ApiHeaders.ContentEncoding);
		DefaultExposedHeaders.add (ApiHeaders.ContentLanguage);
		DefaultExposedHeaders.add (ApiHeaders.ContentRange);
		
		DefaultExposedHeaders.add (ApiHeaders.CacheControl);
		DefaultExposedHeaders.add (ApiHeaders.Expires);
		DefaultExposedHeaders.add (ApiHeaders.LastModified);
		DefaultExposedHeaders.add (ApiHeaders.Pragma);
		DefaultExposedHeaders.add (ApiHeaders.ETag);
		DefaultExposedHeaders.add (ApiHeaders.Cookie);
		DefaultExposedHeaders.add (ApiHeaders.SetCookie);
		
		DefaultExposedHeaders.add (ApiHeaders.Location);

		// bnb
		DefaultExposedHeaders.add (ApiHeaders.NodeID);
		DefaultExposedHeaders.add (ApiHeaders.NodeType);
		DefaultExposedHeaders.add (ApiHeaders.NodeVersion);
		
		DefaultExposedHeaders.add (ApiHeaders.ExecutionTime);
		
		// tus
		DefaultExposedHeaders.add (ApiHeaders.Tus.UploadOffset);
		DefaultExposedHeaders.add (ApiHeaders.Tus.UploadLength);
		DefaultExposedHeaders.add (ApiHeaders.Tus.TusVersion);
		DefaultExposedHeaders.add (ApiHeaders.Tus.TusResumable);
		DefaultExposedHeaders.add (ApiHeaders.Tus.TusExtension);
		DefaultExposedHeaders.add (ApiHeaders.Tus.TusMaxSize);
	}

	private Map<String, ApiRequestBodyReader> readers = new HashMap<String, ApiRequestBodyReader> ();

	private int 		port 		= 80;
	
	private JsonObject 	pool 		= (JsonObject)new JsonObject ().set (Pool.Min, 20).set (Pool.Max, 200);
	
	private String 		context 	= Lang.SLASH;
	private boolean 	gzip 		= true;
	private boolean 	monitor;
	
	// idleTimeout in seconds
	private int			idleTimeout = 30;
	
	private JsonObject	ssl;
	
	private JsonObject	cors;

	private Server 		httpServer;
	
	@Override
	public void init (final ApiServer server) throws Exception {
		
		Log.setLog (new JettyLogger (this));
		
		// load favicon
		InputStream favicon = null;
		try {
			favicon = new FileInputStream (new File (home, FavIcon));
			FavIconContent = IOUtils.toByteArray (favicon);
		} finally {
			IOUtils.closeQuietly (favicon);
		}
		
		Integer poolIdleTimeout 	= Json.getInteger (pool, Pool.IdleTimeout, 300);
		
		/*
		It is very important to limit the task queue of Jetty. By default, the queue is unbounded! As a result, 
		if under high load in excess of the processing power of the webapp, jetty will keep a lot of requests on the queue. 
		Even after the load has stopped, Jetty will appear to have stopped responding to new requests as it still has lots of requests on 
		the queue to handle.
		
		For a high reliability system, it should reject the excess requests immediately (fail fast) by using a queue with 
		a bounded capability. The capability (maximum queue length) should be calculated according to the "no-response" time tolerable. 
		For example, if the webapp can handle 100 requests per second, and if you can allow it one minute to recover from excessive high load, 
		you can set the queue capability to 60*100=6000. If it is set too low, it will reject requests too soon and can't handle normal load 
		spike.
		 */
		
		QueuedThreadPool tp = new QueuedThreadPool (
			Json.getInteger (pool, Pool.Max, 200), 
			Json.getInteger (pool, Pool.Min, 10), 
			poolIdleTimeout * 1000,
			new ArrayBlockingQueue<Runnable> (Json.getInteger (pool, Pool.Capacity, 500))
		);
		tp.setDetailedDump (false);
		tp.setThreadsPriority (Thread.NORM_PRIORITY);

		httpServer = new Server (tp);
		
		ServerConnector connector = new ServerConnector (httpServer);
        connector.setPort (port);
        connector.setIdleTimeout (idleTimeout * 1000);

        httpServer.addConnector (connector);
       
        if (ssl != null && 
        		!Lang.isNullOrEmpty (ssl.getString (Ssl.Keystore)) && 
        		!Lang.isNullOrEmpty (ssl.getString (Ssl.Password))) {
        	
        	HttpConfiguration https = new HttpConfiguration ();
			https.addCustomizer (new SecureRequestCustomizer ());
			
			SslContextFactory sslContextFactory = new SslContextFactory();
			sslContextFactory.setKeyStorePath (new File (ssl.getString (Ssl.Keystore)).getAbsolutePath ());
			sslContextFactory.setKeyStorePassword (Json.getString (ssl, Ssl.Password));
			sslContextFactory.setKeyManagerPassword (Json.getString (ssl, Ssl.StorePassword, Json.getString (ssl, Ssl.Password)));
			sslContextFactory.setKeyStoreType (Json.getString (ssl, Ssl.StoreType, "JKS"));
			
			String [] aCiphers = null;
			
			// include ciphers
			JsonArray ciphers = Json.getArray (Json.getObject (ssl, Ssl.ciphers.class.getSimpleName ()), Ssl.ciphers.Include);
			if (ciphers != null && !ciphers.isEmpty ()) {
				aCiphers = new String [ciphers.count ()];
				for (int i = 0; i < ciphers.count (); i++) {
					aCiphers [i] = (String)ciphers.get (i);
				}
				sslContextFactory.setIncludeCipherSuites (aCiphers);
			}
			// exclude ciphers
			ciphers = Json.getArray (Json.getObject (ssl, Ssl.ciphers.class.getSimpleName ()), Ssl.ciphers.Exclude);
			if (ciphers != null && !ciphers.isEmpty ()) {
				aCiphers = new String [ciphers.count ()];
				for (int i = 0; i < ciphers.count (); i++) {
					aCiphers [i] = (String)ciphers.get (i);
				}
				sslContextFactory.setExcludeCipherSuites (aCiphers);
			}
			
			
			ServerConnector sslConnector = new ServerConnector (httpServer,
					new SslConnectionFactory (sslContextFactory, "http/1.1"),
					new HttpConnectionFactory (https));
            
			int sslPort = Json.getInteger (ssl, Ssl.Port, 443);
			
			sslConnector.setPort (sslPort);
			sslConnector.setIdleTimeout (Json.getInteger (ssl, Ssl.IdleTimeout, idleTimeout) * 1000);
        	
            httpServer.addConnector (sslConnector);
        }
        
        for (Connector cn : httpServer.getConnectors ()) {
            for (ConnectionFactory x  : cn.getConnectionFactories ()) {
    	        if (x instanceof HttpConnectionFactory) {
    	            ((HttpConnectionFactory)x).getHttpConfiguration ().setSendServerVersion (false);
    	            ((HttpConnectionFactory)x).getHttpConfiguration ().setSendXPoweredBy (false);
    	        }
    	    }
        }
		
        ServletContextHandler sContext = new ServletContextHandler (ServletContextHandler.NO_SESSIONS);
        sContext.setContextPath (context);
        
        //sContext.setAttribute ("org.eclipse.jetty.server.webapp.WebInfIncludeJarPattern", ".*/annos-[^/]*\\.jar$");
        
        if (gzip) {
            sContext.setGzipHandler (new GzipHandler ());
        }
        
		httpServer.setHandler (sContext);
		
		ServletHolder apiHolder = 
			new ServletHolder (new HttpServlet () {
	        	
	        	private static final long serialVersionUID = -4391155835460802144L;
	
	        	@Override
				protected void doGet (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	        		if (FavIconPath.equals (req.getRequestURI ())) {
	        			resp.getOutputStream ().write (FavIconContent);
	        			return;
	        		}
	        		execute (req, resp);
	        	}
	
	        	@Override
	        	protected void doPost (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	        		execute (req, resp);
	        	}
	
	        	@Override
	        	protected void doPut (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	        		execute (req, resp);
	        	}
	
	        	@Override
	        	protected void doDelete (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	        		execute (req, resp);
	        	}
	
	        	@Override
				protected void doHead (HttpServletRequest req, HttpServletResponse resp)
						throws ServletException, IOException {
	        		execute (req, resp);
				}

				@Override
				protected void doOptions (HttpServletRequest req, HttpServletResponse resp)
						throws ServletException, IOException {
					execute (req, resp);
				}
				
				@Override
				protected void doTrace (HttpServletRequest req, HttpServletResponse resp)
						throws ServletException, IOException {
					execute (req, resp);
				}
				
				@Override
				public void service (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			        if (req.getMethod ().equalsIgnoreCase (ApiVerb.PATCH.name ())) {
			        	execute (req, resp);
			        } else {
			            super.service (req, resp);
			        }
			    }

				protected void execute (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	        		try {
	        			ApiRequest request = new HttpApiRequest (req, JettyPlugin.this);
	        			request.getNode ().set (ApiRequest.Fields.Node.Id, server.id ());
	        			request.getNode ().set (ApiRequest.Fields.Node.Type, server.type ());
	        			request.getNode ().set (ApiRequest.Fields.Node.Version, server.version ());
	        			server.execute (request, new HttpApiResponse (request.getNode (), request.getId (), resp), CodeExecutor.Mode.Async);
	        		} catch (Exception e) {
	        			throw new ServletException (e.getMessage (), e);
	        		}
	        	}
	        	
	        });

        sContext.addServlet (apiHolder, Lang.SLASH + Lang.STAR);
        
        // cross origin
        FilterHolder holder = new FilterHolder (CrossOriginFilter.class);
        holder.setName ("CORS");
        holder.setInitParameter ("allowedOrigins", Json.getString (cors, Cors.Origins, Lang.STAR));
        holder.setInitParameter ("allowedMethods", Json.getString (cors, Cors.Methods, "GET,POST,HEAD,PUT,DELETE,PATCH,OPTIONS"));
        // allow headers
        String sAllowedHeaders = Lang.STAR;
        JsonArray allowedHeaders = (JsonArray)Json.find (cors, Cors.Headers, Cors.Allow);
        if (!Json.isNullOrEmpty (allowedHeaders)) {
        	sAllowedHeaders = allowedHeaders.join (Lang.COMMA);
        } 
        holder.setInitParameter ("allowedHeaders", sAllowedHeaders);
        
        // exposed headers
        JsonArray exposedHeaders = (JsonArray)Json.find (cors, Cors.Headers, Cors.Expose);
        if (Json.isNullOrEmpty (exposedHeaders)) {
        	exposedHeaders = DefaultExposedHeaders;
        } 
        holder.setInitParameter ("exposedHeaders", exposedHeaders.join (Lang.COMMA));
        
        sContext.addFilter (holder, "/*", EnumSet.of (DispatcherType.INCLUDE, DispatcherType.FORWARD, DispatcherType.REQUEST, DispatcherType.ERROR));
        
        // monitor
		if (monitor) {
			MBeanContainer mbContainer = new MBeanContainer (ManagementFactory.getPlatformMBeanServer ());
			httpServer.addEventListener (mbContainer);
			httpServer.addBean (mbContainer);
			httpServer.addBean (Log.getLog ());
		}
		
		// init default request body readers
		addReader (new StreamApiRequestBodyReader ());
		addReader (new TextApiRequestBodyReader ());
		addReader (new JsonApiRequestBodyReader ());
		addReader (new YamlApiRequestBodyReader ());
        
		// start server
        
		httpServer.start ();

        httpServer.join ();

	}
	
	@Override
	public void kill () {
		if (httpServer == null) {
			return;
		}
		try {
			httpServer.stop ();
		} catch (Exception e) {
			tracer ().log (Tracer.Level.Error, Lang.BLANK, e);
		}
		
		super.kill ();
		
	}

	public int getPort () {
		return port;
	}
	public void setPort (int port) {
		this.port = port;
	}

	public JsonObject getPool() {
		return pool;
	}
	public void setPool(JsonObject pool) {
		this.pool = pool;
	}

	public JsonObject getSsl() {
		return ssl;
	}
	public void setSsl(JsonObject ssl) {
		this.ssl = ssl;
	}

	public JsonObject getCors () {
		return cors;
	}
	public void setCors (JsonObject cors) {
		this.cors = cors;
	}

	public String getContext () {
		return context;
	}
	public void setContext (String context) {
		this.context = context;
	}

	public boolean isGzip () {
		return gzip;
	}
	public void setGzip (boolean gzip) {
		this.gzip = gzip;
	}

	public int getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}
	
	public boolean isMonitor () {
		return monitor;
	}

	public void setMonitor (boolean monitor) {
		this.monitor = monitor;
	}

	public JsonArray getRequestBodyReaders () {
		return null;
	}
	public void setRequestBodyReaders (JsonArray requestBodyReaders) {
		if (Json.isNullOrEmpty (requestBodyReaders)) {
			return;
		}
		for (int i = 0; i < requestBodyReaders.count (); i++) {
			Object o = requestBodyReaders.get (i);
			if (!(o instanceof JsonObject)) {
				continue;
			}
			Object oReader = null;
			try {
				oReader = BeanUtils.create ((JsonObject)o);
			} catch (Exception ex) {
				tracer ().log (Level.Error, ex.getMessage (), ex);
				continue;
			}
			
			if (oReader == null) {
				continue;
			}
			
			if (!(oReader instanceof ApiRequestBodyReader)) {
				continue;
			}
			
			addReader ((ApiRequestBodyReader)oReader);
			
		}
	}
	
	private void addReader (ApiRequestBodyReader reader) {
		if (reader.mediaTypes () == null || reader.mediaTypes ().length == 0) {
			return;
		}
		for (String mt : reader.mediaTypes ()) {
			readers.put (mt, reader);
		}
	}
	
	public ApiRequestBodyReader getReader (String mediaType) {
		return readers.get (mediaType);
	}

}
