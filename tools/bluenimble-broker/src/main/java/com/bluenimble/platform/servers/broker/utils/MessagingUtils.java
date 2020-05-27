package com.bluenimble.platform.servers.broker.utils;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Message;
import com.bluenimble.platform.servers.broker.Response;

public class MessagingUtils {

	public static JsonObject createError (String listener, Object transaction, Object message) {
		return (JsonObject)new JsonObject ()
				.set (Message.Status, Response.Error)
				.set (Message.Listener, listener)
				.set (Message.Transaction, transaction)
				.set (Message.Reason, message);
	}
	
	public static JsonObject createSuccess (String listener, Object transaction, Object message) {
		return (JsonObject)new JsonObject ()
				.set (Message.Status, Response.Success)
				.set (Message.Listener, listener)
				.set (Message.Transaction, transaction)
				.set (Message.Reason, message);
	}
	
}
