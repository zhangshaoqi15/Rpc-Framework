package test.rpc.consumer;

/**
 * 天气查询服务接口
 */
public interface WhetherService {
	
	String selectWhether(String date);
}
