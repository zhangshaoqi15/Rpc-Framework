package test.rpc.consumer;

/**
 * 温度查询服务接口
 */
public interface TemperatureService {
	
	String selectTemper(String time);
}
