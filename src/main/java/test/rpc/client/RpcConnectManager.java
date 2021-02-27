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
 * Rpc连接管理器
 *
 */
@Slf4j
public class RpcConnectManager {

	private static volatile RpcConnectManager CONNECT_MANAGER = new RpcConnectManager();
	private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
	//缓存，每一个IP地址对应一个处理器
	private Map<InetSocketAddress, RpcClientHandler> connectedHandlerMap = new ConcurrentHashMap<>();
	//连接成功的地址的handlerList
	private CopyOnWriteArrayList<RpcClientHandler> connectedHandlerList = new CopyOnWriteArrayList<>();
	//用于异步提交连接请求的线程池
	private ThreadPoolExecutor threadPoolExecutor = 
			new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));
	//可重入锁
	private ReentrantLock connectedLock = new ReentrantLock();
	//条件变量
	private Condition connectedCondition = connectedLock.newCondition();
	//超时时间，3000毫秒
	private long connectTimeoutMills = 3000;
	//运行的标志，默认为运行状态
	private volatile boolean isRunning = true;
	//
	private volatile AtomicInteger hanlderIndex = new AtomicInteger(0);
	
	
	private RpcConnectManager() {}
	
	/**
	 * 单例方式获取连接管理器
	 * @return
	 */
	public static RpcConnectManager getInstance() {
		return CONNECT_MANAGER;
	}
	
	/**
	 * 发起连接
	 * @param serverAddr
	 */
	public void connect(String host, int port) {
		System.out.println("RpcConnectManager connecting: " + host + ":" + port);
		updateConnect(host, port);
	}
	
	/**
	 * 更新缓存中的连接
	 * @param addrList
	 */
	public void updateConnect(String host, int port) {
		//检测传过来的IP地址是否为空
		if(StringUtils.isNotEmpty(host)) {
			Set<InetSocketAddress> newServerSet = new HashSet<>();
			InetSocketAddress peer = new InetSocketAddress(host, port);
			//缓存在set中
			newServerSet.add(peer);
			
			//调用发起连接方法（异步）
			for (InetSocketAddress inetSocketAddress : newServerSet) {
				//判断是否存在已连接的IP地址，不存在才发起连接
				if(!connectedHandlerMap.keySet().contains(inetSocketAddress)) {
					connectAsync(inetSocketAddress);
				}
			}
			
			//删除缓存中不可用的IP地址
			for (int i = 0; i < connectedHandlerList.size(); i++) {
				RpcClientHandler handler = connectedHandlerList.get(i);
				SocketAddress remotePeer = handler.getRemotePeer();
				//判断此时新的连接set中是否有不可用的地址
				if(!newServerSet.contains(remotePeer)) {
					log.info("remove invalid server address——" + remotePeer);
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
	 * 选择一个业务处理器
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public RpcClientHandler chooseHandler() {
		System.out.println("chooseHandler 1");
		//多线程下使用写时复制的clone方法保证线程安全
		CopyOnWriteArrayList<RpcClientHandler> handlers = 
				(CopyOnWriteArrayList<RpcClientHandler>) connectedHandlerList.clone();
		int size = handlers.size();
		
		while (isRunning && size <= 0) {
			try {
				System.out.println("chooseHandler 2");
				boolean isAvailable = waitForHandler();
				System.out.println("waitForHandler isAvailable: " + isAvailable);
				if(isAvailable) {
					//增加连接后，缓存会更新，需要再次clone一份
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
		//这时，当前线程被强制终止，应该返回null
		if(!isRunning) {
			return null;
		}
		
		//原子加1再取模
		int index = (hanlderIndex.getAndAdd(1) + size) % size;
		
		System.out.println("chooseHandler 4");
		return handlers.get(index);
	}
	
	/**
	 * 停止服务
	 */
	public void stop() {
		isRunning = false;
		for (int i = 0; i < connectedHandlerList.size(); i++) {
			//先释放所有资源
			RpcClientHandler handler = connectedHandlerList.get(i);
			handler.close();
		}
		
		//需要唤醒当前正在阻塞的线程，然后释放
		signalHandler();
		threadPoolExecutor.shutdown();
		eventLoopGroup.shutdownGracefully();
	}
	
	/**
	 * 手动重连
	 * @param handler
	 * @param remotePeer
	 */
	public void reconnect(RpcClientHandler handler, SocketAddress remotePeer) {
		if (handler != null) {
			//先释放所有资源
			handler.close();
			connectedHandlerMap.remove(remotePeer);
			connectedHandlerList.remove(handler);
			
		}
		connectAsync((InetSocketAddress) remotePeer);
	}

	/**
	 * 发起连接（异步）
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
						//编码
						cp.addLast(new RpcEncoder(RpcRequest.class))
						//对整个数据包辅助解析
						.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
						//解码
						.addLast(new RpcDecoder(RpcResponse.class))
						//业务处理
						.addLast(new RpcClientHandler());	
					}
					
				});
				
				connect(b, remotePeer);
			}
		});
	}

	/**
	 * 实际处理连接的方法，并且失败后重连
	 * @param b
	 * @param remotePeer
	 */
	private void connect(Bootstrap b, InetSocketAddress remotePeer) {
		System.out.println("connect 1");
		final ChannelFuture cf = b.connect(remotePeer);
		//连接失败后监听，并清除资源
		cf.channel().closeFuture().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				log.info("ChannelFuture channel close, remote peer = " + remotePeer);
				//定时每3秒执行一次runnable任务，每次任务都是清空并调用此方法（重连）
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
		
		//连接成功后监听，并缓存
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
	 * 连接失败需要的释放资源、清空缓存
	 */
	private void clearConnected() {
		for (RpcClientHandler RpcClientHandler : connectedHandlerList) {
			SocketAddress remotePeer = RpcClientHandler.getRemotePeer();
			//取出缓存中的handler
			RpcClientHandler handler = connectedHandlerMap.get(remotePeer);
			if(handler != null) {
				handler.close();
				connectedHandlerMap.remove(remotePeer);
			}
		}
		
		connectedHandlerList.clear();
	}
	

	/**
	 * 把RpcClientHandler添加到指定缓存中
	 * @param handler
	 */
	private void addHandler(RpcClientHandler handler) {
		//异步调用时，有可能通道未激活，无法拿到IP地址
		InetSocketAddress remoteAddress = (InetSocketAddress)handler.getChannel().remoteAddress();
		connectedHandlerMap.put(remoteAddress, handler);
		connectedHandlerList.add(handler);
		//唤醒可用的处理器
		signalHandler();
	}
	

	/**
	 * 等待可用连接
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
	 * 唤醒可用连接
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
