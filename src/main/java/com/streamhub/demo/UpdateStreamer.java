package com.streamhub.demo;

import java.util.HashSet;
import java.util.Set;

import com.streamhub.api.JsonPayload;
import com.streamhub.api.Payload;
import com.streamhub.api.PushServer;
import com.streamhub.util.Sleep;

class UpdateStreamer implements Runnable {
	private final Set<String> topicsToStream = new HashSet<String>();
	private final PushServer streamingServer;
	private boolean isStopped;
	private int sequenceNumber = 0;

	public UpdateStreamer(PushServer streamingServer, int updatesPerSecond) {
		this.streamingServer = streamingServer;
	}

	public void run() {
		Thread.currentThread().setName("UpdateStreamer");
		long error = 0;
		while(! isStopped) {
			long startSecond = System.currentTimeMillis();
			for (int i = 0; i < 10; i++) {
				String topic = "topic-" + i;
				publish(topic);
				int sleepTime = 100;
				if (error > 0 && error < 100) {
					sleepTime -= error;
					error = 0;
				}
				Sleep.millis(sleepTime);
			}
			error = System.currentTimeMillis() - startSecond - 1000;
			System.out.println("error=" + error + "ms");
		}
	}

	private void publish(String topic) {
		if (topicsToStream.contains(topic)) {
			String randomNumber = randomNumber();
			String sequenceNumber = sequenceNumber();
			Payload payload = new JsonPayload(topic);
			payload.addField("sequenceNumber", sequenceNumber);
			payload.addField("randomOne", randomNumber);
			payload.addField("randomTwo", randomNumber);
			payload.toggleTimestamping(true);
			long startPublish = System.currentTimeMillis();
			streamingServer.publish(topic, payload);
			System.out.println("Time in publish() = " + (System.currentTimeMillis() - startPublish) + "ms");
		}
	}
	
	public void addTopic(String topic) {
		synchronized (topicsToStream) {
			topicsToStream.add(topic);
		}
	}
	
	public void stop() {
		isStopped = true;
	}

	private String randomNumber() {
		return Double.toString(Math.random() * 100.0);
	}

	private String sequenceNumber() {
		return "" + sequenceNumber++;
	}
}
