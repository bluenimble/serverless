package com.bluenimble.platform.servers.broker.impls;

import java.io.File;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Tenant;
import com.bluenimble.platform.servers.broker.TenantProvider;
import com.bluenimble.platform.servers.broker.server.Broker;

public class DefaultTenantProvider implements TenantProvider {

	private static final long serialVersionUID = 5362732014000634874L;
	
	protected JsonObject source;
	
	private String tenantsFile;

	public DefaultTenantProvider () {
	}
	
	public DefaultTenantProvider (JsonObject source) {
		this.source = source;
	}
	
	@Override
	public void init (Broker broker, File home) throws Exception {
		
		File file = null;
		
		if (tenantsFile.startsWith ("./")) {
			file = new File (home, tenantsFile.substring (2));
		} else {
			file = new File (tenantsFile);
		}
		
		source = Json.load (file);
	}

	@Override
	public Tenant get (String tenantId) {
		JsonObject oTenant = Json.getObject (source, tenantId);
		if (oTenant == null) {
			return null;
		}
		return new TenantImpl (tenantId, oTenant);
	}

	@Override
	public void add (Tenant tenant) {
		if (tenant == null || Lang.isNullOrEmpty (tenant.id ())) {
			return;
		}
		source.set (tenant.id (), tenant.toJson ());
	}

	@Override
	public void delete (String tenantId) {
		if (Lang.isNullOrEmpty (tenantId)) {
			return;
		}
		source.remove (tenantId);
	}

	public String getTenantsFile () {
		return tenantsFile;
	}
	public void setTenantsFile (String tenantsFile) {
		this.tenantsFile = tenantsFile;
	}

}
