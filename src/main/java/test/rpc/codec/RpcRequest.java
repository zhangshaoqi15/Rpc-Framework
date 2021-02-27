package test.rpc.codec;

import java.io.Serializable;

import lombok.Data;

@Data
public class RpcRequest implements Serializable {

	private static final long serialVersionUID = 1L;
	//数据包的ID
	private String requestId;
	private String className;
	private String methodName;
	//请求的参数类型
	private Class<?>[] paramTypes;
	//请求参数
	private Object[] params;
}
