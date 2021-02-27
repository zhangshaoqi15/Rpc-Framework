package test.rpc.server;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import test.rpc.codec.RpcRequest;
import test.rpc.codec.RpcResponse;

/**
 *  ҵ������
 *
 */
@Slf4j
public class RpcSeverHandler extends SimpleChannelInboundHandler<RpcRequest> {

	private Map<String, Object> handlerMap;
	private ThreadPoolExecutor executor = 
			new ThreadPoolExecutor(16, 16, 1000L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));
	
	public RpcSeverHandler(Map<String, Object> handlerMap) {
		this.handlerMap = handlerMap;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				RpcResponse response = new RpcResponse();
				response.setRequestId(rpcRequest.getRequestId());
				try {
					Object result = handle(rpcRequest);
					response.setResult(result);
				} catch (Throwable t) {
					response.setThrowable(t);
					log.error("rpc service handle error: " + t);
				}
				
				//
				ctx.writeAndFlush(response);
			}
		});
	}

	/**
	 * ��������ͨ�������ȡ����ı���ʵ�����ٵ��÷�����������Ӧ���
	 * @param request
	 * @return
	 * @throws InvocationTargetException
	 */
	private Object handle(RpcRequest request) throws InvocationTargetException {
		String className = request.getClassName();
		Object ref = handlerMap.get(className);
		Class<?> serviceClass = ref.getClass();
		String methodName = request.getMethodName();
		Class<?>[] paramTypes = request.getParamTypes();
		Object[] params = request.getParams();

		//Cglib�������
		FastClass fastClass = FastClass.create(serviceClass);
		FastMethod fastMethod = fastClass.getMethod(methodName, paramTypes);
		Object result = fastMethod.invoke(ref, params);
		
		return result;
	}

	/**
	 * �쳣����
	 */
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.error("caught Throwable" + cause);
		ctx.close();
	}

}
