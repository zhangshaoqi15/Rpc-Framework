package test.registry.loadbalancer;

import java.util.List;

/**
 * ���ؾ���ӿ�
 */
public interface ServiceLoadBalancer<T> {
	T select(List<T> servers, int hashCode);
}
