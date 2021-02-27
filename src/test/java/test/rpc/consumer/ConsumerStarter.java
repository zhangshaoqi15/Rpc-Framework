package test.rpc.consumer;

import test.rpc.client.RpcClient;
import test.rpc.client.RpcFuture;
import test.rpc.client.proxy.RpcAsyncProxy;

public class ConsumerStarter {
	
	/**
	 * 同步调用
	 * @throws Exception 
	 */
	public static void sync() throws Exception {
		int timeout = 3000;
		
		RpcClient rpcClient = new RpcClient(timeout);
		TemperatureService temperatureService = rpcClient.invokeSync(TemperatureService.class);
		String result = temperatureService.selectTemper("12:00");
		System.out.println("result: " +result);		
	}
	
	/**
	 * 异步调用
	 * @throws Exception 
	 */
	public static void async() throws Exception {
		int timeout = 3000;
		
		RpcClient rpcClient = new RpcClient(timeout);
		RpcAsyncProxy proxy = rpcClient.invokeAsync(TemperatureService.class);
		RpcFuture future = proxy.call("selectTemper", "12:00");
		RpcFuture future2 = proxy.call("selectTemper", "23:00");

		Object result = future.get();
		Object result2 = future2.get();
		System.out.println("result: " + result);
		System.out.println("result2: " + result2);

	}
	
	public static void main(String[] args) throws Exception {
		sync();
		//async();
	}
	
}
