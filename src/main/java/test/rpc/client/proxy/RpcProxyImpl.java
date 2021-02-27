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
 * ����ʵ����
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
	 * ����ӿڵ���
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("invoke 1");
		//�����������
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
		//invokerHashCode ����RPC����ӿڲ����б��е�һ�������� hashCode
		int invokerHashCode = params.length > 0 ? params[0].hashCode() : serviceName.hashCode();
		//ͨ����������ע������ʵ��
		RegistryService registryService = RegistryFactory.getRegistryInstance();
		
		//ͨ���������ҵ�����ʵķ���ڵ�
		ServiceMeta serviceMetadata = registryService.discovery(serviceName, invokerHashCode);
		
		String host = serviceMetadata.getServiceAddr();
		int port = serviceMetadata.getServicePort();
		
		//���ӷ���ڵ�
		RpcConnectManager.getInstance().connect(host, port);
		
		//ѡ��clientҵ������
		RpcClientHandler handler = RpcConnectManager.getInstance().chooseHandler();
		
		//�������󣬷��ؽ��
		RpcFuture future = handler.sendRequest(request);
		Object result = future.get(timeout, TimeUnit.SECONDS);
		
		System.out.println("invoke 2");
		return result;
	}

	/**
	 * �첽����ӿڵ���
	 * @throws Exception 
	 */
	@Override
	public RpcFuture call(String methodName, Object... args) throws Exception {
		//��ȡ��������������
		Class<?>[] paramTypes = new Class[args.length];
		for (int i = 0; i < args.length; i++) {
			paramTypes[i] = getClassType(args[i]);
		}
		
		//�����������
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
		//invokerHashCode ����RPC����ӿڲ����б��е�һ�������� hashCode
		int invokerHashCode = params.length > 0 ? params[0].hashCode() : serviceName.hashCode();
		//ͨ����������ע������ʵ��
		RegistryService registryService = RegistryFactory.getRegistryInstance();
		
		//ͨ���������ҵ�����ʵķ���ڵ�
		ServiceMeta serviceMetadata = registryService.discovery(serviceName, invokerHashCode);
		
		String host = serviceMetadata.getServiceAddr();
		int port = serviceMetadata.getServicePort();
		
		//�ͻ�������
		RpcConnectManager.getInstance().connect(host, port);
		
		//ѡ��client��������
		RpcClientHandler handler = RpcConnectManager.getInstance().chooseHandler();
		
		//�������󣨲���Ҫ�ȴ���ȡ�������Ϊ���첽���ã�
		RpcFuture future = handler.sendRequest(request);
		
		return future;
	}

	/**
	 * �Զ����ȡ��������
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
