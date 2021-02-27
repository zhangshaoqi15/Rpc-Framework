package test.rpc.client.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import test.registry.RegistryFactory;
import test.registry.RegistryService;
import test.registry.ServiceMeta;
import test.rpc.client.RpcClientHandler;
import test.rpc.client.RpcConnectManager;
import test.rpc.client.RpcFuture;
import test.rpc.codec.RpcRequest;

/**
 * 代理实现类
 * @param <T>
 */
public class RpcProxyImpl<T> implements InvocationHandler, RpcAsyncProxy {

	private Class<T> clazz;
	private long timeout;
	
	public RpcProxyImpl(Class<T> interfaceClass, long timeout) {
		this.clazz = interfaceClass;
		this.timeout = timeout;
	}

	/**
	 * 代理接口调用
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("invoke 1");
		//设置请求对象
		RpcRequest request = new RpcRequest();
		request.setRequestId(UUID.randomUUID().toString());
		request.setClassName(method.getDeclaringClass().getName());
		request.setMethodName(method.getName());
		request.setParamTypes(method.getParameterTypes());
		request.setParams(args);
		
		String[] className = request.getClassName().split("\\.");
		String serviceVersion = "1.0";
		String serviceName = className[className.length-1] + "-" + serviceVersion;
		Object[] params = request.getParams();
		System.out.println("serviceName: " + serviceName + ", params: " + params);
		//invokerHashCode 采用RPC服务接口参数列表中第一个参数的 hashCode
		int invokerHashCode = params.length > 0 ? params[0].hashCode() : serviceName.hashCode();
		//通过工厂创建注册中心实例
		RegistryService registryService = RegistryFactory.getRegistryInstance();
		
		//通过服务发现找到最合适的服务节点
		ServiceMeta serviceMetadata = registryService.discovery(serviceName, invokerHashCode);
		
		String host = serviceMetadata.getServiceAddr();
		int port = serviceMetadata.getServicePort();
		
		//连接服务节点
		RpcConnectManager.getInstance().connect(host, port);
		
		//选择client业务处理器
		RpcClientHandler handler = RpcConnectManager.getInstance().chooseHandler();
		
		//发送请求，返回结果
		RpcFuture future = handler.sendRequest(request);
		Object result = future.get(timeout, TimeUnit.SECONDS);
		
		System.out.println("invoke 2");
		return result;
	}

	/**
	 * 异步代理接口调用
	 * @throws Exception 
	 */
	@Override
	public RpcFuture call(String methodName, Object... args) throws Exception {
		//获取各方法参数类型
		Class<?>[] paramTypes = new Class[args.length];
		for (int i = 0; i < args.length; i++) {
			paramTypes[i] = getClassType(args[i]);
		}
		
		//设置请求对象
		RpcRequest request = new RpcRequest();
		request.setRequestId(UUID.randomUUID().toString());
		request.setClassName(clazz.getName());
		request.setMethodName(methodName);
		request.setParamTypes(paramTypes);
		request.setParams(args);
		
		String[] className = request.getClassName().split("\\.");
		String serviceVersion = "1.0";
		String serviceName = className[className.length-1] + "-" + serviceVersion;
		Object[] params = request.getParams();
		System.out.println("serviceName: " + serviceName + ", params: " + params);
		//invokerHashCode 采用RPC服务接口参数列表中第一个参数的 hashCode
		int invokerHashCode = params.length > 0 ? params[0].hashCode() : serviceName.hashCode();
		//通过工厂创建注册中心实例
		RegistryService registryService = RegistryFactory.getRegistryInstance();
		
		//通过服务发现找到最合适的服务节点
		ServiceMeta serviceMetadata = registryService.discovery(serviceName, invokerHashCode);
		
		String host = serviceMetadata.getServiceAddr();
		int port = serviceMetadata.getServicePort();
		
		//客户端连接
		RpcConnectManager.getInstance().connect(host, port);
		
		//选择client任务处理器
		RpcClientHandler handler = RpcConnectManager.getInstance().chooseHandler();
		
		//发送请求（不需要等待获取结果，因为是异步调用）
		RpcFuture future = handler.sendRequest(request);
		
		return future;
	}

	/**
	 * 自定义获取参数类型
	 * @param obj
	 * @return
	 */
    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        if (typeName.equals("java.lang.Integer")) {
            return Integer.TYPE;
        } else if (typeName.equals("java.lang.Long")) {
            return Long.TYPE;
        } else if (typeName.equals("java.lang.Float")) {
            return Float.TYPE;
        } else if (typeName.equals("java.lang.Double")) {
            return Double.TYPE;
        } else if (typeName.equals("java.lang.Character")) {
            return Character.TYPE;
        } else if (typeName.equals("java.lang.Boolean")) {
            return Boolean.TYPE;
        } else if (typeName.equals("java.lang.Short")) {
            return Short.TYPE;
        } else if (typeName.equals("java.lang.Byte")) {
            return Byte.TYPE;
        }
        return classType;
    }
    
}
