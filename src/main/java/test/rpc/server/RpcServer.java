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
 * Rpc������
 *
 */
@Slf4j
public class RpcServer {
	
	//�����ṩ�߽ڵ�����
	private String host;
	//�ڵ�˿�
	private int port;
	private EventLoopGroup bossGroup = new NioEventLoopGroup();
	private EventLoopGroup workerGroup = new NioEventLoopGroup();
	//key���ӿ����� 	value���ӿڵ�ʵ������
	private Map<String, Object> handlerMap = new ConcurrentHashMap<>();
	//ע�����ķ���
	private RegistryService registryservice;
	
	public RpcServer(String host, int port, 
			String serviceName, String serviceVersion,
			List<ProviderConfig> providerList) throws Exception {
		this.host = host;
		this.port = port;
		this.registryservice = new ZookeeperRegistryService();
		
		//����
		start();
		
		//���񷢲�
        ServiceMeta serviceMeta = new ServiceMeta();
        serviceMeta.setServiceAddr(host);
        serviceMeta.setServicePort(port);
        serviceMeta.setServiceName(serviceName);
        serviceMeta.setServiceVersion(serviceVersion);
		registryservice.register(serviceMeta);
		
		//����ÿһ��ProviderConfig���ӿڵ�ʵ����ʵ����
		for (ProviderConfig pc : providerList) {
			handlerMap.put(pc.getInterfaceClass(), pc.getRef());
		}
	}
	
	/**
	 * ����
	 * @throws InterruptedException
	 */
	private void start() throws InterruptedException {
		ServerBootstrap sb = new ServerBootstrap();
		sb.group(bossGroup, workerGroup)
		.channel(NioServerSocketChannel.class)
		//sync���к�ack���еĳ���Ϊbacklog
		.option(ChannelOption.SO_BACKLOG, 1024)
		.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel sc) throws Exception {
				ChannelPipeline cp = sc.pipeline();
				//���������ݰ���������
				cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
				//����
				.addLast(new RpcDecoder(RpcRequest.class))
				//����
				.addLast(new RpcEncoder(RpcResponse.class))
				//ҵ����
				.addLast(new RpcSeverHandler(handlerMap));
			}
		});
		
		//��IP�Ͷ˿�
		ChannelFuture cf = sb.bind(host, port).sync();
		//�첽����
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
	 * �ر�
	 */
	public void close() {
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}
	

	
}
