package com.bluenimble.platform.servers.broker.server.impls.api;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.servers.broker.Message;
import com.bluenimble.platform.servers.broker.listeners.EventListener;
import com.bluenimble.platform.servers.broker.server.Broker;
import com.bluenimble.platform.servers.broker.server.RestService;
import com.corundumstudio.socketio.BroadcastOperations;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public class BroadcastRestService implements RestService {
	
	private static final Logger logger = LoggerFactory.getLogger (BroadcastRestService.class);

	@Override
	public FullHttpResponse execute (Broker broker, FullHttpRequest request) {
		
		String payload = request.content ().toString (CharsetUtil.UTF_8);
    	if (Lang.isNullOrEmpty (payload)) {
			ByteBuf content = Unpooled.copiedBuffer ("Invalid Json Payload", CharsetUtil.UTF_8);
			return new DefaultFullHttpResponse (HTTP_1_1, HttpResponseStatus.UNPROCESSABLE_ENTITY, content);
    	}
    	
    	JsonObject oPayload = null;
		try {
    		oPayload = new JsonObject (payload);
		} catch (Exception ex) {
			ByteBuf content = Unpooled.copiedBuffer ("Invalid Json Payload", CharsetUtil.UTF_8);
			return new DefaultFullHttpResponse (HTTP_1_1, HttpResponseStatus.UNPROCESSABLE_ENTITY, content);
		}
		
		String event = Json.getString (oPayload, Message.Event, EventListener.Default.message.name ());
		
		Object oChannel = oPayload.get (Message.Channel);
		if (oChannel == null) {
			ByteBuf content = Unpooled.copiedBuffer ("Invalid Json Payload. Channel required", CharsetUtil.UTF_8);
			return new DefaultFullHttpResponse (HTTP_1_1, HttpResponseStatus.UNPROCESSABLE_ENTITY, content);
		}
		
		logger.error ("BroadcastOperations Found. Send Broadcast");
		
		JsonObject ack 			= new JsonObject ();
		JsonArray  ackChannels 	= new JsonArray ();
		ack.set (Message.Timestamp, Lang.utc ());
		
		if (oChannel instanceof String) {
			BroadcastOperations ops = broker.server ().getRoomOperations ((String)oChannel);
			if (ops == null) {
				ByteBuf content = Unpooled.copiedBuffer ("No Operations found for " + oChannel, CharsetUtil.UTF_8);
				return new DefaultFullHttpResponse (HTTP_1_1, HttpResponseStatus.NOT_FOUND, content);
			}
			ops.sendEvent (event, oPayload.get (Message.Data));
			ack.set (Message.Ack, 1);
			ackChannels.add (oChannel);
			ack.set (Message.Channel, ackChannels);
		} else if (oChannel instanceof JsonArray) {
			JsonArray channels = Json.getArray (oPayload, Message.Channel);
			if (channels.isEmpty ()) {
				ByteBuf content = Unpooled.copiedBuffer ("Invalid Json Payload. Channel required", CharsetUtil.UTF_8);
				return new DefaultFullHttpResponse (HTTP_1_1, HttpResponseStatus.UNPROCESSABLE_ENTITY, content);
			}
			for (int i = 0; i < channels.count (); i++) {
				String sChannel = String.valueOf (channels.get (i));
				BroadcastOperations ops = broker.server ().getRoomOperations (sChannel);
				if (ops == null) {
					logger.error ("No Operations found for " + sChannel);
					continue;
				}
				ops.sendEvent (event, oPayload.get (Message.Data));
				ackChannels.add (sChannel);
			}
			ack.set (Message.Ack, ackChannels.count () < channels.count () ? 0 : 1);
			ack.set (Message.Channel, ackChannels);
		}
		
		ByteBuf content = Unpooled.copiedBuffer (ack.toString (), CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse (HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        response.headers ().set (HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers ().set (HttpHeaderNames.CONTENT_LENGTH, content.readableBytes ());
        
        return response;
        
	}

	@Override
	public boolean isSecure () {
		return true;
	}

}
