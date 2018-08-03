package com.bluenimble.platform.plugins.protocols.tus.impl.upload;

import java.util.UUID;

import org.apache.commons.lang3.Validate;

import com.bluenimble.platform.Lang;

public class UploadIdFactory {

    private String uploadURI = "/";

    public void setUploadURI (String uploadURI) {
        Validate.notNull(uploadURI, "The upload URI cannot be null");
        this.uploadURI = uploadURI;
    }

    public UUID readUploadId (String url) {
    	
    	String sId = null;
    	
    	int indexOfUri = url.indexOf (uploadURI);
    	if (indexOfUri >= 0) {
    		sId = url.substring (indexOfUri + uploadURI.length ());
    	}
    	
    	if (Lang.isNullOrEmpty (sId) || Lang.SLASH.equals (sId)) {
    		return null;
    	}
    	
    	if (sId.startsWith (Lang.SLASH)) {
    		sId = sId.substring (1);
    	}
    	if (sId.endsWith (Lang.SLASH)) {
    		sId = sId.substring (0, sId.length () - 1);
    	}
    	
    	UUID id = null;

    	try {
            id = UUID.fromString (sId);
        } catch (IllegalArgumentException ex) {
            id = null;
        }

        return id;
    }

    public String getUploadURI() {
        return uploadURI;
    }

    public synchronized UUID createId() {
        return UUID.randomUUID();
    }
}
