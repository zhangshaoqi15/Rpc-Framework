package test.rpc.client;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import test.rpc.client.proxy.RpcAsyncProxy;
import test.rpc.client.proxy.RpcProxyImpl;

/**
 * Rpc客户端
 */
public class RpcClient {
	
	private long timeout;
	//同步调用代理实例缓存
	private Map<Class<?>, Object> syncProxyInstanceMap = new ConcurrentHashMap<>();
	//异步调用代理实例缓存
	private Map<Class<?>, Object> asyncProxyInstanceMap = new ConcurrentHashMap<>();
	
	public RpcClient(long timeout) throws Exception {
		this.timeout = timeout;
	}
	
	/**
	 * 同步调用，JDK动态代理生成代理对象，发送Rpc请求
	 * @param <T>
	 * @param interfaceClass
	 * @return
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public <T> T invokeSync(Class<T> interfaceClass) throws Exception {
		System.out.println("invokeSync 1");
		if (syncProxyInstanceMap.containsKey(interfaceClass)) {
			return (T) syncProxyInstanceMap.get(interfaceClass);
		}
		
		//创建代理实例
		Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), 
				new Class<?>[] {interfaceClass}, 
				new RpcProxyImpl<T>(interfaceClass, timeout));
		syncProxyInstanceMap.put(interfaceClass, proxy);
		
		System.out.println("invokeSync 2");
		return (T) proxy;
	}
	
	/**
	 * 异步调用，用RpcProxyImpl实现
	 * @param <T>
	 * @param interfaceClass
	 * @return
	 */
	public <T> RpcAsyncProxy invokeAsync(Class<T> interfaceClass) {
		System.out.println("invokeAsync 1");
		if (asyncProxyInstanceMap.containsKey(interfaceClass)) {
			return (RpcAsyncProxy) asyncProxyInstanceMap.get(interfaceClass);
		}
		
		RpcProxyImpl<T> RpcProxyInstance = new RpcProxyImpl<>(interfaceClass, timeout);
		asyncProxyInstanceMap.put(interfaceClass, RpcProxyInstance);
		
		return RpcProxyInstance;
	}
	
	private void stop() {
		RpcConnectManager.getInstance().stop();
	}
	
}
