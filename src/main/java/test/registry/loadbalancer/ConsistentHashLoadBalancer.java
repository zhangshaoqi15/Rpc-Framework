package test.registry.loadbalancer;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.curator.x.discovery.ServiceInstance;

import test.registry.ServiceMeta;

/**
 * 一致性hash算法
 */
public class ConsistentHashLoadBalancer implements ServiceLoadBalancer<ServiceInstance<ServiceMeta>> {

	//虚拟节点数
	private final static int VIRTUAL_NODE_SIZE = 10;
	
	@Override
	public ServiceInstance<ServiceMeta> select(List<ServiceInstance<ServiceMeta>> servers, int hashCode) {
		//用treeMap作为哈希环，key：服务节点的hash值		value：服务实例
		TreeMap<Integer, ServiceInstance<ServiceMeta>> ring = new TreeMap<>();
		
		System.out.println("servers.size: " + servers.size());
		//在哈希环中为每个服务实例创建节点
		for (ServiceInstance<ServiceMeta> serviceInstance : servers) {
			for (int i = 0; i < VIRTUAL_NODE_SIZE; i++) {
				ServiceMeta payload = serviceInstance.getPayload();
				String serviceName = payload.getServiceAddr() 
									+ ":" 
									+ payload.getServicePort() 
									+ "-" + i;
				ring.put(serviceName.hashCode(), serviceInstance);
				System.out.println(serviceName.hashCode()+"");
			}
		}
		
		//分配节点，按顺时针找到大于或等于客户端 hashCode 的第一个节点
		Map.Entry<Integer, ServiceInstance<ServiceMeta>> entry = ring.ceilingEntry(hashCode);
		//如果没有找到，则默认分配环中第一个节点
		if (entry == null) {
			entry = ring.firstEntry();
		}
		
		System.out.println(entry.getValue());
		return entry.getValue();
	}

}
