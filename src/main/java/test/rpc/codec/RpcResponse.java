package test.rpc.codec;

import java.io.Serializable;

import lombok.Data;

@Data
public class RpcResponse implements Serializable {

	private static final long serialVersionUID = 1L;
	//数据包ID，server响应时需要带上原来的requestID
	private String requestId;
	//响应结果
	private Object result;
	//响应异常结果
	private Throwable throwable;
	
}
