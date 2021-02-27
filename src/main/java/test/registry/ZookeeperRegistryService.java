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
 * ע������
 */
public class ZookeeperRegistryService implements RegistryService {

	//private static final String SERVER_ADDR = "127.0.0.1:2181";
	private static final String SERVER_ADDR = "192.168.5.128:2181";
	private static final int BASE_SLEEP_TIME_MS = 2000;
	private static final int MAX_RETRIES = 3;
	private static final String ZK_BASE_PATH = "/create_rpc";
	private final ServiceDiscovery<ServiceMeta> serviceDiscovery;
	
	/**
	 * ���� Zookeeeper �ͻ���
	 * @param registryAddr
	 * @throws Exception
	 */
	public ZookeeperRegistryService() throws Exception {
		System.out.println("ZookeeperRegistryService 1");
		//ͨ�� Factory ���ù���ģʽ���� CuratorFramework ʵ��
		//ָ�����Բ���
		CuratorFramework client = CuratorFrameworkFactory.newClient(SERVER_ADDR, 
				new ExponentialBackoffRetry(BASE_SLEEP_TIME_MS, MAX_RETRIES));
		client.start();
		
		JsonInstanceSerializer<ServiceMeta> serializer = 
				new JsonInstanceSerializer<>(ServiceMeta.class);
		
		//���� ServiceDiscovery ʵ��
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
	 * ����ע��
	 */
	@Override
	public void register(ServiceMeta serviceMeta) throws Exception {
		//����ʵ��
		ServiceInstance<ServiceMeta> serviceInstance = ServiceInstance
				.<ServiceMeta>builder()
				.name(serviceMeta.getServiceName()+"-"+serviceMeta.getServiceVersion())
				.address(serviceMeta.getServiceAddr())
				.port(serviceMeta.getServicePort())
				.payload(serviceMeta)	//����Ԫ����
				.build();
		serviceDiscovery.registerService(serviceInstance);
		System.out.println("successfully register!");
		System.out.println("serviceInstance.getName: " + serviceInstance.getName());
	}

	/**
	 * ����ע��
	 */
	@Override
	public void unRegister(ServiceMeta serviceMeta) throws Exception {
		ServiceInstance<ServiceMeta> serviceInstance = ServiceInstance
				.<ServiceMeta>builder()
				.name(serviceMeta.getServiceName())
				.address(serviceMeta.getServiceAddr())
				.port(serviceMeta.getServicePort())
				.payload(serviceMeta)	//����Ԫ����
				.build();
		serviceDiscovery.unregisterService(serviceInstance);
		System.out.println("successfully unRegister!");
	}

	/**
	 * ������
	 */
	@Override
	public ServiceMeta discovery(String serviceName, int invokerHashCode) throws Exception {
		System.out.println("discovery 1");
		//��ȡ����ڵ��б�
		Collection<ServiceInstance<ServiceMeta>> servers = 
				serviceDiscovery.queryForInstances(serviceName);
		//�ҵ���Ӧ�ķ���ڵ�
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
	 * ע����������
	 */
	@Override
	public void destroy() throws IOException {
		serviceDiscovery.close();
	}

}
