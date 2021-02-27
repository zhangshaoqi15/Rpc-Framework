package test.rpc.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import test.registry.RegistryService;
import test.registry.ServiceMeta;
import test.registry.ZookeeperRegistryService;
import test.rpc.codec.RpcDecoder;
import test.rpc.codec.RpcEncoder;
import test.rpc.codec.RpcRequest;
import test.rpc.codec.RpcResponse;
import test.rpc.config.ProviderConfig;

/**
 * Rpc服务器
 *
 */
@Slf4j
public class RpcServer {
	
	//服务提供者节点主机
	private String host;
	//节点端口
	private int port;
	private EventLoopGroup bossGroup = new NioEventLoopGroup();
	private EventLoopGroup workerGroup = new NioEventLoopGroup();
	//key：接口名字 	value：接口的实例对象
	private Map<String, Object> handlerMap = new ConcurrentHashMap<>();
	//注册中心服务
	private RegistryService registryservice;
	
	public RpcServer(String host, int port, 
			String serviceName, String serviceVersion,
			List<ProviderConfig> providerList) throws Exception {
		this.host = host;
		this.port = port;
		this.registryservice = new ZookeeperRegistryService();
		
		//启动
		start();
		
		//服务发布
        ServiceMeta serviceMeta = new ServiceMeta();
        serviceMeta.setServiceAddr(host);
        serviceMeta.setServicePort(port);
        serviceMeta.setServiceName(serviceName);
        serviceMeta.setServiceVersion(serviceVersion);
		registryservice.register(serviceMeta);
		
		//缓存每一个ProviderConfig（接口的实现类实例）
		for (ProviderConfig pc : providerList) {
			handlerMap.put(pc.getInterfaceClass(), pc.getRef());
		}
	}
	
	/**
	 * 启动
	 * @throws InterruptedException
	 */
	private void start() throws InterruptedException {
		ServerBootstrap sb = new ServerBootstrap();
		sb.group(bossGroup, workerGroup)
		.channel(NioServerSocketChannel.class)
		//sync队列和ack队列的长度为backlog
		.option(ChannelOption.SO_BACKLOG, 1024)
		.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel sc) throws Exception {
				ChannelPipeline cp = sc.pipeline();
				//对整个数据包辅助解析
				cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
				//解码
				.addLast(new RpcDecoder(RpcRequest.class))
				//编码
				.addLast(new RpcEncoder(RpcResponse.class))
				//业务处理
				.addLast(new RpcSeverHandler(handlerMap));
			}
		});
		
		//绑定IP和端口
		ChannelFuture cf = sb.bind(host, port).sync();
		//异步监听
		cf.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					log.info("server binding successfully, addr: " + host + ":" + port);
				}
				else {
					log.info("server binding failed, addr: " + host + ":" + port);
					throw new Exception("server start failed, cause: " + future.cause());
				}
			}
		});
		
	}
	
	/**
	 * 关闭
	 */
	public void close() {
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}
	

	
}
