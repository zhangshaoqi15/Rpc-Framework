package test.rpc.config;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

/**
 *  配置抽象类
 *
 */
public abstract class AbstractRPCConfig {

	private AtomicInteger generator = new AtomicInteger();
	private String id;
	private String interfaceClass;
	//consumer独有属性
	private Class<?> proxyClass;
	
	public String getId() {
		if(StringUtils.isBlank(id)) {
			id = "config-" + generator.getAndIncrement();
		}
		
		return id;
	}
	
	public String getInterfaceClass() {
		return interfaceClass;
	}

	public void setInterfaceClass(String interfaceClass) {
		this.interfaceClass = interfaceClass;
	}

	public void setId(String id) {
		this.id = id;
	}
	
}
