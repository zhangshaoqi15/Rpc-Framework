package test.registry;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import test.registry.loadbalancer.ConsistentHashLoadBalancer;

/**
 * 注册中心
 */
public class ZookeeperRegistryService implements RegistryService {

	//private static final String SERVER_ADDR = "127.0.0.1:2181";
	private static final String SERVER_ADDR = "192.168.5.128:2181";
	private static final int BASE_SLEEP_TIME_MS = 2000;
	private static final int MAX_RETRIES = 3;
	private static final String ZK_BASE_PATH = "/create_rpc";
	private final ServiceDiscovery<ServiceMeta> serviceDiscovery;
	
	/**
	 * 构建 Zookeeeper 客户端
	 * @param registryAddr
	 * @throws Exception
	 */
	public ZookeeperRegistryService() throws Exception {
		System.out.println("ZookeeperRegistryService 1");
		//通过 Factory 采用工厂模式创建 CuratorFramework 实例
		//指定重试策略
		CuratorFramework client = CuratorFrameworkFactory.newClient(SERVER_ADDR, 
				new ExponentialBackoffRetry(BASE_SLEEP_TIME_MS, MAX_RETRIES));
		client.start();
		
		JsonInstanceSerializer<ServiceMeta> serializer = 
				new JsonInstanceSerializer<>(ServiceMeta.class);
		
		//创建 ServiceDiscovery 实例
		serviceDiscovery = ServiceDiscoveryBuilder
							.builder(ServiceMeta.class)
							.client(client)
							.serializer(serializer)
							.basePath(ZK_BASE_PATH)
							.build();
		serviceDiscovery.start();
		
		System.out.println("ZookeeperRegistryService 2");
	}
	
	/**
	 * 服务注册
	 */
	@Override
	public void register(ServiceMeta serviceMeta) throws Exception {
		//服务实例
		ServiceInstance<ServiceMeta> serviceInstance = ServiceInstance
				.<ServiceMeta>builder()
				.name(serviceMeta.getServiceName()+"-"+serviceMeta.getServiceVersion())
				.address(serviceMeta.getServiceAddr())
				.port(serviceMeta.getServicePort())
				.payload(serviceMeta)	//服务元数据
				.build();
		serviceDiscovery.registerService(serviceInstance);
		System.out.println("successfully register!");
		System.out.println("serviceInstance.getName: " + serviceInstance.getName());
	}

	/**
	 * 服务注销
	 */
	@Override
	public void unRegister(ServiceMeta serviceMeta) throws Exception {
		ServiceInstance<ServiceMeta> serviceInstance = ServiceInstance
				.<ServiceMeta>builder()
				.name(serviceMeta.getServiceName())
				.address(serviceMeta.getServiceAddr())
				.port(serviceMeta.getServicePort())
				.payload(serviceMeta)	//服务元数据
				.build();
		serviceDiscovery.unregisterService(serviceInstance);
		System.out.println("successfully unRegister!");
	}

	/**
	 * 服务发现
	 */
	@Override
	public ServiceMeta discovery(String serviceName, int invokerHashCode) throws Exception {
		System.out.println("discovery 1");
		//获取服务节点列表
		Collection<ServiceInstance<ServiceMeta>> servers = 
				serviceDiscovery.queryForInstances(serviceName);
		//找到相应的服务节点
		ServiceInstance<ServiceMeta> serviceInstance = 
				new ConsistentHashLoadBalancer()
				.select((List<ServiceInstance<ServiceMeta>>) servers, invokerHashCode);
		
		if (serviceInstance == null) {
			return null;
		}
		
		System.out.println("successfully discovery!");
		return serviceInstance.getPayload();
	}

	/**
	 * 注册中心销毁
	 */
	@Override
	public void destroy() throws IOException {
		serviceDiscovery.close();
	}

}
