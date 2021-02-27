package test.rpc.client;

public interface RpcCallback {

	void success(Object result);
	void failure(Throwable t);
}
