package test.rpc.client.proxy;

import test.rpc.client.RpcFuture;

/**
 * 异步代理接口
 */
public interface RpcAsyncProxy {
	
	RpcFuture call(String methodName, Object... args) throws Exception;
	
}
