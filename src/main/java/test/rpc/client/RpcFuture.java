package test.rpc.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;
import test.rpc.codec.RpcRequest;
import test.rpc.codec.RpcResponse;

/**
 * RPCFutureģ��
 */
@Slf4j
public class RpcFuture implements Future<Object> {

	private RpcRequest request;
	private RpcResponse response;
	//�Զ�������������ס����RpcFuture�ĸ����߳�
	private SyncLock syncLock;
	//rpc���ÿ�ʼʱ��
	private long startTime;
	//���õ���ֵ
	private static final long THRESHOLD = 5000L;
	//�ȴ����õĻص���������
	private List<RpcCallback> callbackList = new ArrayList<>();
	//������ס���ûص��������߳�
	private ReentrantLock lock = new ReentrantLock();
	//�̳߳أ�����ִ�лص�����
	private ThreadPoolExecutor executor = 
			new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));
	
	public RpcFuture(RpcRequest request) {
		this.request = request;
		this.syncLock = new SyncLock();
		this.startTime = System.currentTimeMillis();
	}

	/**
	 * ʵ�ʵĻص�����
	 * @param response
	 */
	public void done(RpcResponse response) {
		this.response = response;
		if(syncLock.release(1)) {
			invokeCallbacks();
		}
		
		//����Rpc���õĺ�ʱ
		long costTime = System.currentTimeMillis() - startTime;
		if (costTime > THRESHOLD) {
			log.warn("time was too long! Cost Time: " + costTime);
		}
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCancelled() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDone() {
		return syncLock.isDone();
	}

	/**
	 * ��ȡ��Ӧ���
	 */
	@Override
	public Object get() throws InterruptedException, ExecutionException {
		//��ȡ��ռģʽ���������������߳�
		syncLock.acquire(-1);
		if (response != null) {
			return response.getResult();
		}

		return null;
	}

	/**
	 * ��ȡ��Ӧ���������ʱʱ��
	 */
	@Override
	public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		//ͬ���ǻ�ȡ��ռģʽ����
		boolean isSuccess = syncLock.tryAcquireNanos(-1, unit.toNanos(timeout));
		if (isSuccess && response != null) {
			return response.getResult();
		}
		else {
			throw new RuntimeException("timeout exception!");
		}
	}
	
	/**
	 * �������лص�����
	 */
	private void invokeCallbacks() {
		lock.lock();
		try {
			for (RpcCallback callback : callbackList) {
				runCallback(callback);
			}
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * ִ�лص�����
	 * @param callback
	 */
	private void runCallback(RpcCallback callback) {
		RpcResponse response = this.response;
		executor.submit(new Runnable() {
			@Override
			public void run() {
				if(response.getThrowable() == null) {
					callback.success(response.getResult());
				}
				else {
					callback.failure(response.getThrowable());
				}
			}
		});
	}

	/**
	 * �Զ��������̳�AQS
	 */
	static class SyncLock extends AbstractQueuedSynchronizer {

		private static final long serialVersionUID = 1L;
		//�����
		private static final int done = 1;
		//�ȴ���
		private static final int pending = 0;
		
		/**
		 * ���Ի�ȡ���������ж����Ƿ���У���ʵ�ʻ�ȡ���ķ���
		 */
		@Override
		protected boolean tryAcquire(int arg) {
			return getState() == done;
		}
		
		/**
		 * �����ͷ����������ж����Ƿ�ȴ��ͷţ���ʵ���ͷ����ķ���
		 */
		@Override
		protected boolean tryRelease(int arg) {
			if (getState() == pending) {
				//CAS�ķ�ʽ��state��ǰֵ�޸�Ϊdone���޸ĳɹ������ͷ����ɹ�
				if(compareAndSetState(pending, done)) {
					return true;
				}
			}
			return false;
		}
		
		public boolean isDone() {
			return getState() == done;
		}
		
	}

}
