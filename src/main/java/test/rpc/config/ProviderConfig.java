package test.rpc.config;

/**
 * 接口的实现类实例
 */
public class ProviderConfig extends AbstractRPCConfig {

	private Object ref;

	public Object getRef() {
		return ref;
	}

	public void setRef(Object ref) {
		this.ref = ref;
	}
}
