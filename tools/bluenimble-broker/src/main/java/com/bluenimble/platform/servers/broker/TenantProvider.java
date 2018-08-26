package com.bluenimble.platform.servers.broker;

import java.io.File;
import java.io.Serializable;

import com.bluenimble.platform.servers.broker.server.Broker;

public interface TenantProvider extends Serializable {

	void	init	(Broker broker, File home) throws Exception;
	Tenant 	get 	(String tenantId);
	void 	add 	(Tenant tenant);
	void 	delete 	(String tenantId);
}
