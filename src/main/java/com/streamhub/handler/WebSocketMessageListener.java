package com.streamhub.handler;

import com.streamhub.StreamingSubscriptionManager;
import com.streamhub.api.SubscriptionManager;
import com.streamhub.reader.MessageListener;

public class WebSocketMessageListener implements MessageListener {
	private StreamingSubscriptionManager subscriptionManager;

	public WebSocketMessageListener(SubscriptionManager subscriptionManager) {
		this.subscriptionManager = (StreamingSubscriptionManager) subscriptionManager;
	}

	public void onMessage(String message) {
		// TODO Auto-generated method stub
		
	}

}
