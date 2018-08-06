package com.bluenimble.platform.servers.socketio.listeners.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.socketio.listeners.AbstractListener;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;

public class RestAuthorizationListener extends AbstractListener implements AuthorizationListener {
	
	private static final Logger logger = LoggerFactory.getLogger (RestAuthorizationListener.class);
	
	interface Spec {
		String Endpoint 		= "endpoint";
		String Type 			= "type";
		
		String Peers			= "peers";
		String Key 				= "key";
		
		// only apply if type == Form
		String PeerIdField 		= "peerId";
		String PeerKeyField		= "peerKey";
		String PeerTypeField 	= "peerType";
	}

	interface AuthType {
		String Form 	= "form";
		String Basic 	= "basic";
	}
	
	interface Defaults {
		String PeerField 	= "peer";
		String KeyField 	= "key";
		String TypeField 	= "type";
	}
	
	private JsonObject spec;
	
	public RestAuthorizationListener (JsonObject spec) {
		super (null);
		this.spec = spec;
	}
	
	@Override
	public boolean isAuthorized (HandshakeData data) {
		
		String type = getProperty (Spec.Type, AuthType.Basic);
		
		String token = data.getSingleUrlParam (AbstractListener.Spec.Peer.Token);
		if (Lang.isNullOrEmpty(token)) {
			return false;
		}

		token = token.trim ();
		
		logger.info ("Auth Token: " + token);
		
		int indexOfColon = token.indexOf (Lang.COLON);
		if (indexOfColon <= 0) {
			return false;
		}
		
		String peer = token.substring (0, indexOfColon);
		String key 	= token.substring (indexOfColon + 1);
		if (Lang.isNullOrEmpty (peer) || Lang.isNullOrEmpty (key)) {
			return false;
		}
		
		JsonObject oPeers = Json.getObject (spec, Spec.Peers);
		
		if (Json.find (oPeers, peer) != null) {
			logger.info ("Peer found in master peers");
			boolean valid = key.equals (Json.find (oPeers, peer, Spec.Key));
			if (valid) {
				data.getUrlParams ().put (AbstractListener.Spec.Peer.Type, Arrays.asList ((String)Json.find (oPeers, peer, Spec.Type)));
			}
			return valid;
		}
		
		try {
			try (CloseableHttpClient client = HttpClients.createDefault ()) {
				
				logger.info ("Create a POST request to " + getProperty (Spec.Endpoint, null));
			    
				HttpPost httpPost = new HttpPost (getProperty (Spec.Endpoint, null));
				
				httpPost.addHeader (ApiHeaders.Accept, ApiContentTypes.Json);
			    
			    if (AuthType.Basic.equals (type)) {
					logger.info ("Add Basic auth header");
				    UsernamePasswordCredentials creds
				      = new UsernamePasswordCredentials (peer, key);
				    
				    httpPost.addHeader (new BasicScheme ().authenticate (creds, httpPost, null));
				    
			    } else if (AuthType.Form.equals (type)) {
					logger.info ("Add Form auth filds");
				    List<NameValuePair> params = new ArrayList<NameValuePair>();
				    params.add (new BasicNameValuePair (getProperty (Spec.PeerIdField, Defaults.PeerField), peer));
				    params.add (new BasicNameValuePair (getProperty (Spec.PeerKeyField, Defaults.KeyField), key));
				    
				    httpPost.setEntity (new UrlEncodedFormEntity (params));
			    }
			    
			    CloseableHttpResponse response = client.execute (httpPost);
				
				int statusCode = response.getStatusLine ().getStatusCode (); 
				if (statusCode != 200) { 
					logger.info ("Status code " + statusCode);
					return false;
				}
				
				String responseText = EntityUtils.toString (response.getEntity ());
				logger.info ("Response Type " + response.getFirstHeader (ApiHeaders.ContentType));
				
				logger.info ("Response Text " + responseText);
				
				JsonObject rPeer = new JsonObject (responseText);
				
				data.getUrlParams ().put (AbstractListener.Spec.Peer.Type, Arrays.asList (Json.getString (rPeer, Spec.Type)));
				
			}
		} catch (Exception ex) {
			return false;
		}
		
		return true;

	}
	
	private String getProperty (String name, String defaultValue) {
		if (!spec.containsKey (name)) {
			return defaultValue;
		}
		return spec.getString (name);
	}

	@Override
	public void onData (SocketIOClient client, JsonObject data, AckRequest ackSender) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
}