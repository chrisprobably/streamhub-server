package com.streamhub.request;

import java.util.ArrayList;
import java.util.List;

import com.streamhub.api.Payload;
import com.streamhub.tools.MockDirectClient;


public class LatencyMeasuringDirectClient extends MockDirectClient {
	public static final String TIMESTAMP_FIELD = "Timestamp";
	private List<Long> latencies = new ArrayList<Long>();

	public LatencyMeasuringDirectClient(String uid) {
		super(uid);
	}

	@Override
	public void onMessage(String message) {
		long receiveTime = System.currentTimeMillis();

		if (message.contains(TIMESTAMP_FIELD)) {
			Payload payload = UrlEncodedJsonPayload.createFrom(message);
			long sendTime = Long.parseLong(payload.getFields().get(TIMESTAMP_FIELD));
			long latency = receiveTime - sendTime;
			latencies.add(latency);
		}
		
		super.onMessage(message);
	}
	
	public double getAverageLatency() {
		double numLatencyMessages = latencies.size();
		double totalLatency = 0;
		for (Long latency : latencies) {
			totalLatency += latency;
		}
		
		return totalLatency / numLatencyMessages;
	}
}
