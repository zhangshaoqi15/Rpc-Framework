package test.rpc.codec;

import java.io.Serializable;

import lombok.Data;

@Data
public class RpcRequest implements Serializable {

	private static final long serialVersionUID = 1L;
	//���ݰ���ID
	private String requestId;
	private String className;
	private String methodName;
	//����Ĳ�������
	private Class<?>[] paramTypes;
	//�������
	private Object[] params;
}
