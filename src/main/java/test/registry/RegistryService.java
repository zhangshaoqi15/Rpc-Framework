package test.registry;

import java.io.IOException;

/**
 * 注册中心接口
 */
public interface RegistryService {
	
	//服务注册
	void register(ServiceMeta serviceMeta) throws Exception;;
    //void register(ServiceMeta serviceMeta) throws Exception;
    //服务注销
    void unRegister(ServiceMeta serviceMeta) throws Exception;
    //服务发现
    ServiceMeta discovery(String serviceName, int invokerHashCode) throws Exception;
    //注册中心销毁
    void destroy() throws IOException;
    
}