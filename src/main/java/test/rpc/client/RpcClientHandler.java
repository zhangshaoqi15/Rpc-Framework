package test.rpc.client;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import test.rpc.codec.RpcRequest;
import test.rpc.codec.RpcResponse;

/**
 * 业务处理器
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
	
	private Channel channel;
	private SocketAddress remotePeer;
	//等待处理的缓存
	//key：请求的ID	value：RpcFuture实例
	private Map<String, RpcFuture> pendingTable = new ConcurrentHashMap<>(); 
	
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
		channel = ctx.channel();
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		remotePeer = channel.remoteAddress();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
		String requestId = response.getRequestId();
		RpcFuture RpcFuture = pendingTable.get(requestId);
		//判断是否已有响应结果
		if(RpcFuture != null) {
			pendingTable.remove(requestId);
			RpcFuture.done(response);
		}
	}
	
	public SocketAddress getRemotePeer() {
		return remotePeer;
	}
	
	public Channel getChannel() {
		return channel;
	}
	
	public void close() {
		//channel发送一个EMPTY_BUFFER，并为CLOSE事件添加监听器，马上触发事件关闭channel，是一种主动关闭连接的方式
		channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
	}
	
	/**
	 *  异步发送请求
	 * @param request
	 * @return
	 * @throws Exception 
	 */
	public RpcFuture sendRequest(RpcRequest request) throws Exception {
		System.out.println("sendRequest 1");
		RpcFuture RpcFuture = new RpcFuture(request);
		
		pendingTable.put(request.getRequestId(), RpcFuture);
		channel.writeAndFlush(request);
		
		System.out.println("sendRequest 2");
		return RpcFuture;
	}

}
