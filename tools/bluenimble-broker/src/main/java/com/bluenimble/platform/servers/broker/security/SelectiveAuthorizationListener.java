package com.bluenimble.platform.servers.broker.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.HandshakeData;

public class SelectiveAuthorizationListener implements AuthorizationListener {
	
	protected Map<String, List<AuthorizationListener>> listeners = new LinkedHashMap<String, List<AuthorizationListener>> ();
	
	protected String context;
	
	public SelectiveAuthorizationListener (String context) {
		this.context = context;
	}
	
	@Override
	public boolean isAuthorized (HandshakeData data) {
		
		String namespace = Lang.SLASH;
		
		String url = data.getUrl ();
		
		url = Lang.replace (url, context, Lang.BLANK);
		
		int indexOfQMark = url.indexOf (Lang.QMARK);
		if (indexOfQMark > 0) {
			namespace = url.substring (0, indexOfQMark);
		} else if (indexOfQMark != 0) {
			namespace = url;
		}
		
		List<AuthorizationListener> auths = listeners.get (namespace);
		if (auths == null || auths.isEmpty ()) {
			return false;
		}
		
		for (AuthorizationListener auth : auths) {
			boolean isAuthorized = auth.isAuthorized (data);
			if (isAuthorized) {
				return true;
			}
		}
		
		return false;
	}
	
	public void addListener (String name, AuthorizationListener listener) {
		if (listener == null) {
			return;
		}
		List<AuthorizationListener> list = listeners.get (name);
		if (list == null) {
			list = new ArrayList<AuthorizationListener> ();
			listeners.put (name, list);
		}
		list.add (listener);
	}

}
