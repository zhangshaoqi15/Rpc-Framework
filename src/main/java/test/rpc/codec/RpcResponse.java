package test.rpc.codec;

import java.io.Serializable;

import lombok.Data;

@Data
public class RpcResponse implements Serializable {

	private static final long serialVersionUID = 1L;
	//���ݰ�ID��server��Ӧʱ��Ҫ����ԭ����requestID
	private String requestId;
	//��Ӧ���
	private Object result;
	//��Ӧ�쳣���
	private Throwable throwable;
	
}
