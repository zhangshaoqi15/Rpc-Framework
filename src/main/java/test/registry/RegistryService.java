package test.registry;

import java.io.IOException;

/**
 * ע�����Ľӿ�
 */
public interface RegistryService {
	
	//����ע��
	void register(ServiceMeta serviceMeta) throws Exception;;
    //void register(ServiceMeta serviceMeta) throws Exception;
    //����ע��
    void unRegister(ServiceMeta serviceMeta) throws Exception;
    //������
    ServiceMeta discovery(String serviceName, int invokerHashCode) throws Exception;
    //ע����������
    void destroy() throws IOException;
    
}