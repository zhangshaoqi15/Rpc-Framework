package test.rpc.provider;

import java.util.ArrayList;
import java.util.List;

import test.rpc.config.ProviderConfig;
import test.rpc.server.RpcServer;

public class ProviderStarter {
	
	public static void main(String[] args) {
		
		//	服务端启动
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					// 每一个具体的服务提供者的配置类
					ProviderConfig pc1 = new ProviderConfig();
					pc1.setInterfaceClass("test.rpc.consumer.TemperatureService");
					TemperatureServiceImpl temperatureServiceImpl = TemperatureServiceImpl.class.newInstance();
					pc1.setRef(temperatureServiceImpl);
					
					ProviderConfig pc2 = new ProviderConfig();
					pc2.setInterfaceClass("test.rpc.consumer.WhetherService");
					WhetherServiceImpl whetherServiceImpl = WhetherServiceImpl.class.newInstance();
					pc2.setRef(whetherServiceImpl);
					
					//	把所有的ProviderConfig 添加到集合中
					List<ProviderConfig> providerConfigs = new ArrayList<ProviderConfig>();
					providerConfigs.add(pc1);
					providerConfigs.add(pc2);
					
					String host = "127.0.0.1";
					int port = 9999;
					String serviceName = "TemperatureService";
					String serviceVersion = "1.0";
					
					new RpcServer(host, port, serviceName, serviceVersion, providerConfigs);
					
				} catch(Exception e){
					e.printStackTrace();
				}	
			}
		}).start();
		
	}
}
