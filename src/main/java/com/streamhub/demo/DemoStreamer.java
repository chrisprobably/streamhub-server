package com.streamhub.demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.streamhub.api.Client;
import com.streamhub.api.PushServer;
import com.streamhub.api.SubscriptionListener;

class DemoStreamer implements SubscriptionListener {
	private final int updatesPerSecond;
	private boolean isStarted;
	private final ExecutorService threadPool = Executors.newCachedThreadPool();
	private final PushServer streamingServer;
	private UpdateStreamer updateStreamer;

	public DemoStreamer(PushServer streamingServer, int updatesPerSecond) {
		this.streamingServer = streamingServer;
		this.updatesPerSecond = updatesPerSecond;
		streamingServer.getSubscriptionManager().addSubscriptionListener(this);
	}

	public void onSubscribe(String topic, Client client) {
		synchronized (this) {
			if (! isStarted) {
				isStarted = true;
				updateStreamer = new UpdateStreamer(streamingServer, updatesPerSecond);
				threadPool.execute(updateStreamer);
			}
			
			updateStreamer.addTopic(topic);
		}
	}

	public void onUnSubscribe(String topic, Client client) {
		
	}
	
	public void stop() {
		threadPool.shutdownNow();
		if (updateStreamer != null) {
			updateStreamer.stop();
		}
	}
}
