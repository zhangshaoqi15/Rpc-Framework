package test.rpc.provider;

import test.rpc.consumer.TemperatureService;

public class TemperatureServiceImpl implements TemperatureService{

	@Override
	public String selectTemper(String time) {
		String res = "the temperature of " + time + " is 20°„C";
		return res;
	}
	
}
