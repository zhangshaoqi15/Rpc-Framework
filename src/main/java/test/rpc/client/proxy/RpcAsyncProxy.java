package test.rpc.client.proxy;

import test.rpc.client.RpcFuture;

/**
 * �첽����ӿ�
 */
public interface RpcAsyncProxy {
	
	RpcFuture call(String methodName, Object... args) throws Exception;
	
}
