package com.streamhub.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.streamhub.Connection;
import com.streamhub.api.PublishListener;
import com.streamhub.api.SubscriptionListener;
import com.streamhub.api.SubscriptionManager;

class RemoteAdapterRepository implements UIDRepository, ConnectionListener {

	private final Map<String,PublishListener> uidToPublishListener = new HashMap<String, PublishListener>();
	private final Map<String,SubscriptionListener> uidToSubscriptionListener = new HashMap<String, SubscriptionListener>();
	private final SubscriptionManager subscriptionManager;
	
	public RemoteAdapterRepository(SubscriptionManager subscriptionManager) {
		this.subscriptionManager = subscriptionManager;
	}

	public PublishListener findOrCreatePublishListener(String uid, Connection connection) {
		PublishListener listener; 
		
		if (!uidToPublishListener.containsKey(uid)) {
			listener = new RemotePublishListener(connection, this);
			uidToPublishListener.put(uid, listener);
		} else {
			listener = uidToPublishListener.get(uid);
		}
		
		return listener;
	}

	public SubscriptionListener findOrCreateSubscriptionListener(String uid, Connection connection) {
		SubscriptionListener listener; 
		
		if (!uidToSubscriptionListener.containsKey(uid)) {
			listener = new RemoteSubscriptionListener(connection, this);
			uidToSubscriptionListener.put(uid, listener);
		} else {
			listener = uidToSubscriptionListener.get(uid);
		}
		
		return listener;
	}

	public void connectionLost(SubscriptionListener subscriptionListener) {
		subscriptionManager.removeSubscriptionListener(subscriptionListener);
		removeSubscriptionListenerFromRepo(subscriptionListener);
	}

	public void connectionLost(PublishListener publishListener) {
		subscriptionManager.removePublishListener(publishListener);
		removePublishListenerFromRepo(publishListener);
	}
	
	Map<String,PublishListener> getPublishListeners()
	{
		return Collections.unmodifiableMap(uidToPublishListener);
	}
	
	Map<String,SubscriptionListener> getSubscriptionListeners()
	{
		return Collections.unmodifiableMap(uidToSubscriptionListener);
	}
	
	private void removeSubscriptionListenerFromRepo(SubscriptionListener subscriptionListener) {
		String removalKey = null;
		
		for (Map.Entry<String, SubscriptionListener> entry : uidToSubscriptionListener.entrySet()) {
			if (subscriptionListener.equals(entry.getValue())) {
				removalKey = entry.getKey();
				break;
			}
		}
		
		if (removalKey != null) {
			uidToSubscriptionListener.remove(removalKey);
		}
	}
	
	private void removePublishListenerFromRepo(PublishListener publishListener) {
		String removalKey = null;
		
		for (Map.Entry<String, PublishListener> entry : uidToPublishListener.entrySet()) {
			if (publishListener.equals(entry.getValue())) {
				removalKey = entry.getKey();
				break;
			}
		}
		
		if (removalKey != null) {
			uidToPublishListener.remove(removalKey);
		}
	}
}
