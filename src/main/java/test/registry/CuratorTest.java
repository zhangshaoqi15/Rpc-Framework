package test.registry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import test.registry.RegistryFactory;
import test.registry.RegistryService;
import test.registry.ServiceMeta;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * zk的测试类，不属于此项目
 */
public class CuratorTest {
	
	private RegistryService registryService;

    @Before
    public void init() throws Exception {
	    registryService = RegistryFactory.getRegistryInstance();
    }
    
    @Test
    public void testAll() throws Exception {
        ServiceMeta serviceMeta1 = new ServiceMeta();
        serviceMeta1.setServiceAddr("127.0.0.1");
        serviceMeta1.setServicePort(9999);
        serviceMeta1.setServiceName("test1");
        serviceMeta1.setServiceVersion("1.0");
        
        registryService.register(serviceMeta1);
        
        ServiceMeta discovery1 = registryService.discovery("test1-1.0", "test1".hashCode());
        
        assert discovery1 != null;
        
        registryService.unRegister(discovery1);
        
        /*
		CuratorFramework client = CuratorFrameworkFactory.newClient(SERVER_ADDR, 
				new ExponentialBackoffRetry(BASE_SLEEP_TIME_MS, MAX_RETRIES));
		client.start();
		*/
		
		/*
		client
		.create()
		.withMode(CreateMode.PERSISTENT)
		.forPath(ZK_BASE_PATH, "test".getBytes());*/
		
		/*
		byte[] bytes = client.getData().forPath(ZK_BASE_PATH);
		System.out.println(new String(bytes));    	
		*/
    }
	
	
    @After
    public void close() throws Exception {
        registryService.destroy();
    }
}
