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
 * RPCFuture模型
 */
@Slf4j
public class RpcFuture implements Future<Object> {

	private RpcRequest request;
	private RpcResponse response;
	//自定义锁，用于锁住处理RpcFuture的各个线程
	private SyncLock syncLock;
	//rpc调用开始时间
	private long startTime;
	//调用的阈值
	private static final long THRESHOLD = 5000L;
	//等待调用的回调函数集合
	private List<RpcCallback> callbackList = new ArrayList<>();
	//用于锁住调用回调函数的线程
	private ReentrantLock lock = new ReentrantLock();
	//线程池，用于执行回调函数
	private ThreadPoolExecutor executor = 
			new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));
	
	public RpcFuture(RpcRequest request) {
		this.request = request;
		this.syncLock = new SyncLock();
		this.startTime = System.currentTimeMillis();
	}

	/**
	 * 实际的回调处理
	 * @param response
	 */
	public void done(RpcResponse response) {
		this.response = response;
		if(syncLock.release(1)) {
			invokeCallbacks();
		}
		
		//整个Rpc调用的耗时
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
	 * 获取响应结果
	 */
	@Override
	public Object get() throws InterruptedException, ExecutionException {
		//获取独占模式的锁，阻塞其他线程
		syncLock.acquire(-1);
		if (response != null) {
			return response.getResult();
		}

		return null;
	}

	/**
	 * 获取响应结果，带超时时间
	 */
	@Override
	public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		//同样是获取独占模式的锁
		boolean isSuccess = syncLock.tryAcquireNanos(-1, unit.toNanos(timeout));
		if (isSuccess && response != null) {
			return response.getResult();
		}
		else {
			throw new RuntimeException("timeout exception!");
		}
	}
	
	/**
	 * 调用所有回调函数
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
	 * 执行回调函数
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
	 * 自定义锁，继承AQS
	 */
	static class SyncLock extends AbstractQueuedSynchronizer {

		private static final long serialVersionUID = 1L;
		//已完成
		private static final int done = 1;
		//等待中
		private static final int pending = 0;
		
		/**
		 * 尝试获取锁，用于判断锁是否空闲，非实际获取锁的方法
		 */
		@Override
		protected boolean tryAcquire(int arg) {
			return getState() == done;
		}
		
		/**
		 * 尝试释放锁，用于判断锁是否等待释放，非实际释放锁的方法
		 */
		@Override
		protected boolean tryRelease(int arg) {
			if (getState() == pending) {
				//CAS的方式将state当前值修改为done，修改成功就则释放锁成功
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
