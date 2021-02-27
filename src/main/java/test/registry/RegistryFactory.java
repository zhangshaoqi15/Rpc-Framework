package test.registry;

public class RegistryFactory {
	
	private static volatile RegistryService registryService;
	
	public static RegistryService getRegistryInstance() throws Exception {
		if (registryService == null) {
			synchronized (RegistryFactory.class) {
				if (registryService == null) {
					registryService = new ZookeeperRegistryService();
				}
			}
		}
		
		return registryService;
	}
	
	
	
}
