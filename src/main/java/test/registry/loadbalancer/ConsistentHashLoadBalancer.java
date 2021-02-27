package test.registry.loadbalancer;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.curator.x.discovery.ServiceInstance;

import test.registry.ServiceMeta;

/**
 * һ����hash�㷨
 */
public class ConsistentHashLoadBalancer implements ServiceLoadBalancer<ServiceInstance<ServiceMeta>> {

	//����ڵ���
	private final static int VIRTUAL_NODE_SIZE = 10;
	
	@Override
	public ServiceInstance<ServiceMeta> select(List<ServiceInstance<ServiceMeta>> servers, int hashCode) {
		//��treeMap��Ϊ��ϣ����key������ڵ��hashֵ		value������ʵ��
		TreeMap<Integer, ServiceInstance<ServiceMeta>> ring = new TreeMap<>();
		
		System.out.println("servers.size: " + servers.size());
		//�ڹ�ϣ����Ϊÿ������ʵ�������ڵ�
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
		
		//����ڵ㣬��˳ʱ���ҵ����ڻ���ڿͻ��� hashCode �ĵ�һ���ڵ�
		Map.Entry<Integer, ServiceInstance<ServiceMeta>> entry = ring.ceilingEntry(hashCode);
		//���û���ҵ�����Ĭ�Ϸ��价�е�һ���ڵ�
		if (entry == null) {
			entry = ring.firstEntry();
		}
		
		System.out.println(entry.getValue());
		return entry.getValue();
	}

}
