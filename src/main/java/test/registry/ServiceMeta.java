package test.registry;

import lombok.Data;

/**
 * ����Ԫ����
 */
@Data
public class ServiceMeta {
	
	//��������
    private String serviceName;
    //����汾
    private String serviceVersion;
    //�����ַ
    private String serviceAddr;
    //����˿ں�
    private int servicePort;
    
}