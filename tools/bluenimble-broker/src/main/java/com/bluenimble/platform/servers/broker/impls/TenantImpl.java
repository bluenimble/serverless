package com.bluenimble.platform.servers.broker.impls;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Tenant;

public class TenantImpl implements Tenant {

	private static final long serialVersionUID = 7302175755954900067L;

	interface Spec {
		String Id 			= "id";
		String Name 		= "name";
		
		String Available 	= "available";
		String NamespacedBroadcast 	
							= "namespacedBroadcast";
		
		String Auth			= "auth";
		String Events 		= "events";
	}
	
	private JsonObject 	source;
	
	public TenantImpl (String id, JsonObject source) {
		this.source = (JsonObject)source.set (Spec.Id, id);
	}

	@Override
	public String id () {
		return Json.getString (source, Spec.Id);
	}

	@Override
	public String name () {
		return Json.getString (source, Spec.Name);
	}

	@Override
	public boolean supports (String event) {
		return Json.getArray (source, Spec.Events).contains (event);
	}

	@Override
	public boolean available () {
		return Json.getBoolean (source, Spec.Available, true);
	}
	
	@Override
	public String authListener () {
		return Json.getString (source, Spec.Auth);
	}

	@Override
	public JsonObject toJson () {
		return source;
	}

	@Override
	public boolean namespacedBroadcast () {
		return Json.getBoolean (source, Spec.NamespacedBroadcast, false);
	}
	
}
