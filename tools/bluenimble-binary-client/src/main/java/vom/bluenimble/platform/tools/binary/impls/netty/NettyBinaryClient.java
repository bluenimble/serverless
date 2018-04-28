package vom.bluenimble.platform.tools.binary.impls.netty;

import java.util.Map;

import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import vom.bluenimble.platform.tools.binary.BinaryClient;
import vom.bluenimble.platform.tools.binary.BinaryClientException;
import vom.bluenimble.platform.tools.binary.Callback;

public class NettyBinaryClient implements BinaryClient {

	private static final long serialVersionUID = -7191583878115216336L;

	private ChannelHandlerContext 	context;
	private DefaultHandler			handler;

	private EventLoopGroup 			group;
	
	public NettyBinaryClient (String host, int port) throws BinaryClientException {
		
		handler = new DefaultHandler ();
		
		group = new NioEventLoopGroup ();
		Bootstrap b = new Bootstrap ();
		b.group (group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline p = ch.pipeline();
				/*if (sslCtx != null) {
					p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
				}*/
				p.addLast (
					new ObjectEncoder (), 
					new ObjectDecoder (ClassResolvers.cacheDisabled (null)),
					handler
				);
			}
		});

		try {
			b.connect (host, port).sync ();
		} catch (InterruptedException e) {
			throw new BinaryClientException (e.getMessage (), e);
		}
	}

	@Override
	public void send (ApiRequest request) {
		context.writeAndFlush (request);
	}

	@Override
	public void recycle () {
		handler.callback = null;
	}
	
	public void destroy () {
		group.shutdownGracefully ();
        group = null;
	}
	
	void useCallback (Callback callback) {
		handler.callback = callback;
	}

	class DefaultHandler extends ChannelInboundHandlerAdapter {
		
		Callback callback = null;

		@Override
	    public void channelActive (ChannelHandlerContext context) {
			NettyBinaryClient.this.context = context;
	    }

	    @SuppressWarnings("unchecked")
		@Override
	    public void channelRead (ChannelHandlerContext ctx, Object msg) {
	    	//System.out.println (callback);
	    	if (msg instanceof ApiResponse.Status) {
		    	callback.onStatus ((ApiResponse.Status)msg);
	    	} else if (msg instanceof Map) {
		    	callback.onHeaders ((Map<String, Object>)msg);
	    	} else if (msg instanceof byte []) {
		    	callback.onChunk ((byte [])msg);
	    	} 
	    }

	    @Override
	    public void channelReadComplete (ChannelHandlerContext ctx) {
	    	callback.onFinish ();
    		ctx.flush ();
	    }

	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	        cause.printStackTrace();
	        ctx.close ();
	    }

	}

}
