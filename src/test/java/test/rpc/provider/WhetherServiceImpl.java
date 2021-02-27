package test.rpc.provider;

import test.rpc.consumer.WhetherService;

public class WhetherServiceImpl implements WhetherService {

	@Override
	public String selectWhether(String date) {
		String res = "the whether of " + date + " is sunny";
		return res;
	}

}
