package com.bluenimble.platform.icli.mgm.remote;

import java.io.Serializable;

import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.http.response.HttpResponse;

public interface ResponseReader extends Serializable {

	CommandResult read (Tool tool, String contentType, HttpResponse response) throws Exception; 
	
}
