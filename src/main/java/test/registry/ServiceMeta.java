package test.registry;

import lombok.Data;

/**
 * 服务元数据
 */
@Data
public class ServiceMeta {
	
	//服务名称
    private String serviceName;
    //服务版本
    private String serviceVersion;
    //服务地址
    private String serviceAddr;
    //服务端口号
    private int servicePort;
    
}