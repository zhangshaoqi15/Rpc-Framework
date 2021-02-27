package test.rpc.client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import test.rpc.codec.RpcDecoder;
import test.rpc.codec.RpcEncoder;
import test.rpc.codec.RpcRequest;
import test.rpc.codec.RpcResponse;

/**
 * Rpc���ӹ�����
 *
 */
@Slf4j
public class RpcConnectManager {

	private static volatile RpcConnectManager CONNECT_MANAGER = new RpcConnectManager();
	private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
	//���棬ÿһ��IP��ַ��Ӧһ��������
	private Map<InetSocketAddress, RpcClientHandler> connectedHandlerMap = new ConcurrentHashMap<>();
	//���ӳɹ��ĵ�ַ��handlerList
	private CopyOnWriteArrayList<RpcClientHandler> connectedHandlerList = new CopyOnWriteArrayList<>();
	//�����첽�ύ����������̳߳�
	private ThreadPoolExecutor threadPoolExecutor = 
			new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));
	//��������
	private ReentrantLock connectedLock = new ReentrantLock();
	//��������
	private Condition connectedCondition = connectedLock.newCondition();
	//��ʱʱ�䣬3000����
	private long connectTimeoutMills = 3000;
	//���еı�־��Ĭ��Ϊ����״̬
	private volatile boolean isRunning = true;
	//
	private volatile AtomicInteger hanlderIndex = new AtomicInteger(0);
	
	
	private RpcConnectManager() {}
	
	/**
	 * ������ʽ��ȡ���ӹ�����
	 * @return
	 */
	public static RpcConnectManager getInstance() {
		return CONNECT_MANAGER;
	}
	
	/**
	 * ��������
	 * @param serverAddr
	 */
	public void connect(String host, int port) {
		System.out.println("RpcConnectManager connecting: " + host + ":" + port);
		updateConnect(host, port);
	}
	
	/**
	 * ���»����е�����
	 * @param addrList
	 */
	public void updateConnect(String host, int port) {
		//��⴫������IP��ַ�Ƿ�Ϊ��
		if(StringUtils.isNotEmpty(host)) {
			Set<InetSocketAddress> newServerSet = new HashSet<>();
			InetSocketAddress peer = new InetSocketAddress(host, port);
			//������set��
			newServerSet.add(peer);
			
			//���÷������ӷ������첽��
			for (InetSocketAddress inetSocketAddress : newServerSet) {
				//�ж��Ƿ���������ӵ�IP��ַ�������ڲŷ�������
				if(!connectedHandlerMap.keySet().contains(inetSocketAddress)) {
					connectAsync(inetSocketAddress);
				}
			}
			
			//ɾ�������в����õ�IP��ַ
			for (int i = 0; i < connectedHandlerList.size(); i++) {
				RpcClientHandler handler = connectedHandlerList.get(i);
				SocketAddress remotePeer = handler.getRemotePeer();
				//�жϴ�ʱ�µ�����set���Ƿ��в����õĵ�ַ
				if(!newServerSet.contains(remotePeer)) {
					log.info("remove invalid server address����" + remotePeer);
					RpcClientHandler oldHandler = connectedHandlerMap.get(remotePeer);
					if(oldHandler != null) {
						oldHandler.close();
						connectedHandlerMap.remove(remotePeer);
					}
					connectedHandlerList.remove(handler);
				}
			}
			
		}
		else {
			log.error("no availble address");
			clearConnected();
		}
			
	}
	
	/**
	 * ѡ��һ��ҵ������
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public RpcClientHandler chooseHandler() {
		System.out.println("chooseHandler 1");
		//���߳���ʹ��дʱ���Ƶ�clone������֤�̰߳�ȫ
		CopyOnWriteArrayList<RpcClientHandler> handlers = 
				(CopyOnWriteArrayList<RpcClientHandler>) connectedHandlerList.clone();
		int size = handlers.size();
		
		while (isRunning && size <= 0) {
			try {
				System.out.println("chooseHandler 2");
				boolean isAvailable = waitForHandler();
				System.out.println("waitForHandler isAvailable: " + isAvailable);
				if(isAvailable) {
					//�������Ӻ󣬻������£���Ҫ�ٴ�cloneһ��
					handlers = (CopyOnWriteArrayList<RpcClientHandler>) connectedHandlerList.clone();
					size = handlers.size();
					System.out.println("chooseHandler connectedHandlerList.size" + size);
				}
			} catch (InterruptedException e) {
				log.error("waiting for available node has been interrupted");
				throw new RuntimeException("no connected servers", e);
			}
		}
		
		System.out.println("chooseHandler 3");
		//��ʱ����ǰ�̱߳�ǿ����ֹ��Ӧ�÷���null
		if(!isRunning) {
			return null;
		}
		
		//ԭ�Ӽ�1��ȡģ
		int index = (hanlderIndex.getAndAdd(1) + size) % size;
		
		System.out.println("chooseHandler 4");
		return handlers.get(index);
	}
	
	/**
	 * ֹͣ����
	 */
	public void stop() {
		isRunning = false;
		for (int i = 0; i < connectedHandlerList.size(); i++) {
			//���ͷ�������Դ
			RpcClientHandler handler = connectedHandlerList.get(i);
			handler.close();
		}
		
		//��Ҫ���ѵ�ǰ�����������̣߳�Ȼ���ͷ�
		signalHandler();
		threadPoolExecutor.shutdown();
		eventLoopGroup.shutdownGracefully();
	}
	
	/**
	 * �ֶ�����
	 * @param handler
	 * @param remotePeer
	 */
	public void reconnect(RpcClientHandler handler, SocketAddress remotePeer) {
		if (handler != null) {
			//���ͷ�������Դ
			handler.close();
			connectedHandlerMap.remove(remotePeer);
			connectedHandlerList.remove(handler);
			
		}
		connectAsync((InetSocketAddress) remotePeer);
	}

	/**
	 * �������ӣ��첽��
	 * @param inetSocketAddress
	 */
	private void connectAsync(InetSocketAddress remotePeer) {
		System.out.println("connectAsync");
		threadPoolExecutor.submit(new Runnable() {
			@Override
			public void run() {
				Bootstrap b = new Bootstrap();
				b.group(eventLoopGroup)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel sc) throws Exception {
						ChannelPipeline cp = sc.pipeline();
						//����
						cp.addLast(new RpcEncoder(RpcRequest.class))
						//���������ݰ���������
						.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
						//����
						.addLast(new RpcDecoder(RpcResponse.class))
						//ҵ����
						.addLast(new RpcClientHandler());	
					}
					
				});
				
				connect(b, remotePeer);
			}
		});
	}

	/**
	 * ʵ�ʴ������ӵķ���������ʧ�ܺ�����
	 * @param b
	 * @param remotePeer
	 */
	private void connect(Bootstrap b, InetSocketAddress remotePeer) {
		System.out.println("connect 1");
		final ChannelFuture cf = b.connect(remotePeer);
		//����ʧ�ܺ�������������Դ
		cf.channel().closeFuture().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				log.info("ChannelFuture channel close, remote peer = " + remotePeer);
				//��ʱÿ3��ִ��һ��runnable����ÿ����������ղ����ô˷�����������
				future.channel().eventLoop().schedule(new Runnable() {
					@Override
					public void run() {
						log.warn("connect failed, to be reconnect...");
						clearConnected();
						connect(b, remotePeer);
					}
				}, 
				3, TimeUnit.SECONDS);
			}

		});
		
		System.out.println("connect 2");
		
		//���ӳɹ��������������
		cf.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					log.info("connected successfully!");
					RpcClientHandler handler = future.channel().pipeline().get(RpcClientHandler.class);
					addHandler(handler);
				}
			}

		});
		
		System.out.println("connect 3");
	}
	
	/**
	 * ����ʧ����Ҫ���ͷ���Դ����ջ���
	 */
	private void clearConnected() {
		for (RpcClientHandler RpcClientHandler : connectedHandlerList) {
			SocketAddress remotePeer = RpcClientHandler.getRemotePeer();
			//ȡ�������е�handler
			RpcClientHandler handler = connectedHandlerMap.get(remotePeer);
			if(handler != null) {
				handler.close();
				connectedHandlerMap.remove(remotePeer);
			}
		}
		
		connectedHandlerList.clear();
	}
	

	/**
	 * ��RpcClientHandler��ӵ�ָ��������
	 * @param handler
	 */
	private void addHandler(RpcClientHandler handler) {
		//�첽����ʱ���п���ͨ��δ����޷��õ�IP��ַ
		InetSocketAddress remoteAddress = (InetSocketAddress)handler.getChannel().remoteAddress();
		connectedHandlerMap.put(remoteAddress, handler);
		connectedHandlerList.add(handler);
		//���ѿ��õĴ�����
		signalHandler();
	}
	

	/**
	 * �ȴ���������
	 * @return
	 * @throws InterruptedException
	 */
	private boolean waitForHandler() throws InterruptedException {
		System.out.println("waitForHandler");
		connectedLock.lock();
		try {
			return connectedCondition.await(connectTimeoutMills, TimeUnit.MILLISECONDS);
		} 
		finally {
			connectedLock.unlock();
		}
		
	}
	
	/**
	 * ���ѿ�������
	 */
	private void signalHandler() {
		System.out.println("signalHandler");
		connectedLock.lock();
		try {
			connectedCondition.signalAll();
		} 
		finally {
			connectedLock.unlock();
		}
		
	}
	
}
