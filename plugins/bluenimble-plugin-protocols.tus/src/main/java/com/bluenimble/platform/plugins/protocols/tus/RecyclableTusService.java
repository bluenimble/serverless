package com.bluenimble.platform.plugins.protocols.tus;

import java.io.IOException;

import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.plugins.protocols.tus.TusProtocolPlugin.OwnerPlaceholder;
import com.bluenimble.platform.plugins.protocols.tus.impl.TusFileUploadService;

public class RecyclableTusService implements Recyclable {

	private static final long serialVersionUID = 7883791473072921865L;
	
	private TusFileUploadService 	service;
	private Tracer 					tracer;
	
	private String 					tenantKey;
	private OwnerPlaceholder 		tenantPlaceholder;

	public RecyclableTusService (TusFileUploadService service, String tenantKey, OwnerPlaceholder tenantPlaceholder, Tracer tracer) {
		this.service 			= service;
		this.tenantKey 			= tenantKey;
		this.tenantPlaceholder 	= tenantPlaceholder;
	}
	
	@Override
	public void finish (boolean withError) {
		
	}

	@Override
	public void recycle () {
		try {
			service.cleanup ();
		} catch (IOException e) {
			tracer.log (Tracer.Level.Error, e.getMessage (), e);
		}
	}
	
	public TusFileUploadService service () {
		return service;
	}
	
	public String tenantKey () {
		return tenantKey;
	}

	public OwnerPlaceholder tenantPlaceholder () {
		return tenantPlaceholder;
	}

}
