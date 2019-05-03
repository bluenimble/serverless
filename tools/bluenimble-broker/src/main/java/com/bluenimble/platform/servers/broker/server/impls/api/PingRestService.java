package com.bluenimble.platform.servers.broker.server.impls.api;

import com.bluenimble.platform.api.ApiContentTypes;
import com.bluenimble.platform.servers.broker.server.Broker;
import com.bluenimble.platform.servers.broker.server.RestService;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public class PingRestService implements RestService {

	@Override
	public FullHttpResponse execute (Broker broker, FullHttpRequest request) {
		ByteBuf content = Unpooled.copiedBuffer (broker.describe ().toString (), CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse (HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        response.headers ().set (HttpHeaderNames.CONTENT_TYPE, ApiContentTypes.Json);
        response.headers ().set (HttpHeaderNames.CONTENT_LENGTH, content.readableBytes ());
		return response;
	}

	@Override
	public boolean isSecure () {
		return false;
	}

}
