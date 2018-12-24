package com.bluenimble.platform.servers.broker.security;

import java.util.ArrayList;
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

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Peer;
import com.bluenimble.platform.servers.broker.utils.PeerUtils;
import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.HandshakeData;

public class RestAuthorizationListener implements AuthorizationListener {
	
	private static final Logger logger = LoggerFactory.getLogger (RestAuthorizationListener.class);
	
	interface Params {
		String Token = "token";
	}
	
	interface AuthType {
		String Form 	= "form";
		String Basic 	= "basic";
	}
	
	private String type 			= AuthType.Basic;
	private String endpoint;
	
	// if type = form
	private String userField 		= "user";
	private String passwordField 	= "password";
	
	@Override
	public boolean isAuthorized (HandshakeData data) {
		
		String token = data.getSingleUrlParam (Params.Token);
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
		
		try {
			try (CloseableHttpClient client = HttpClients.createDefault ()) {
				
				logger.info ("Create a POST request to " + endpoint);
			    
				HttpPost httpPost = new HttpPost (endpoint);
				
				httpPost.addHeader (ApiHeaders.Accept, ApiContentTypes.Json);
				httpPost.addHeader (ApiHeaders.UserAgent, "BlueNimble Broker - Authorization Listener");
			    
			    if (AuthType.Basic.equals (type)) {
					logger.info ("Add Basic auth header");
				    UsernamePasswordCredentials creds
				      = new UsernamePasswordCredentials (peer, key);
				    
				    httpPost.addHeader (new BasicScheme ().authenticate (creds, httpPost, null));
				    
			    } else if (AuthType.Form.equals (type)) {
					logger.info ("Add Form auth filds");
				    List<NameValuePair> params = new ArrayList<NameValuePair>();
				    params.add (new BasicNameValuePair (userField, peer));
				    params.add (new BasicNameValuePair (passwordField, key));
				    
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
				
				data.getUrlParams ().put (Peer.Key, PeerUtils.toList (peer, rPeer));
				
			}
		} catch (Exception ex) {
			logger.error (ex.getMessage (), ex);
			return false;
		}
		
		return true;

	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getUserField() {
		return userField;
	}

	public void setUserField(String userField) {
		this.userField = userField;
	}

	public String getPasswordField() {
		return passwordField;
	}

	public void setPasswordField(String passwordField) {
		this.passwordField = passwordField;
	}
	
}