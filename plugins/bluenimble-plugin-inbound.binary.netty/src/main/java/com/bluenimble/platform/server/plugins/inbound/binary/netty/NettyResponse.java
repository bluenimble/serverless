package com.bluenimble.platform.server.plugins.inbound.binary.netty;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.impls.AbstractApiResponse;
import com.bluenimble.platform.json.JsonObject;

import io.netty.channel.ChannelHandlerContext;

public class NettyResponse extends AbstractApiResponse {

	private static final long serialVersionUID = -2164934807466002434L;
	
	private ChannelHandlerContext 	context;
	
	private boolean					headersWritten;
	
	private OutputStream			out;
	private Writer					writer;
	
	public NettyResponse (String id, JsonObject node, ChannelHandlerContext context) {
		super (id, node);
		this.context = context;
	}

	@Override
	protected ApiResponse append (byte [] buff, int offset, int length) throws IOException {
		context.write (buff);
		return this;
	}

	@Override
	public OutputStream toOutput () throws IOException {
		
		if (out == null) {
			out = new OutputStream () {
				@Override
			    public void write (byte [] bytes, int off, int len) throws IOException {
			    	context.write (Arrays.copyOfRange (bytes, off, len - off -1));
			    }
				@Override
			    public void write (byte [] bytes) throws IOException {
			    	context.write (bytes);
			    }
				@Override
				public void write (int b) throws IOException {
				}
				@Override
				public void close () throws IOException {
					NettyResponse.this.close ();
				}
			};
		}
		
		return out;
		
	}

	@Override
	public Writer toWriter () throws IOException {
		
		if (writer == null) {
			writer = new Writer () {
				@Override
				public void write (char cbuf []) throws IOException {
					context.write (IOUtils.charsToBytes (cbuf));
				}
				
				@Override
				public void write (char [] cbuf, int off, int len) throws IOException {
					context.write (IOUtils.charsToBytes (Arrays.copyOfRange (cbuf, off, len - off -1)));
				}
	
			    @Override
				public void flush () throws IOException {
				}
	
				@Override
				public void close () throws IOException {
					NettyResponse.this.close ();
				}
			};
			
		}
		
		return writer;
		
	}

	@Override
	public void reset () {
	}

	@Override
	public void setBuffer (int size) {
	}

	@Override
	public void flushHeaders () {
		if (headersWritten) {
			return;
		}
		
		if (status != null) {
			context.write (status.getCode ());
			status = null;
		}
		
		if (headers == null || headers.isEmpty ()) {
			return;
		}
		
		context.write (headers);
		
		headersWritten = true;
	}

	@Override
	public void close () throws IOException {
		context.flush ();
		commit ();
	}

}
