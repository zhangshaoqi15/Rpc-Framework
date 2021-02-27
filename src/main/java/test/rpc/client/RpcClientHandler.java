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
 * ҵ������
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
	
	private Channel channel;
	private SocketAddress remotePeer;
	//�ȴ�����Ļ���
	//key�������ID	value��RpcFutureʵ��
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
		//�ж��Ƿ�������Ӧ���
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
		//channel����һ��EMPTY_BUFFER����ΪCLOSE�¼���Ӽ����������ϴ����¼��ر�channel����һ�������ر����ӵķ�ʽ
		channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
	}
	
	/**
	 *  �첽��������
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
