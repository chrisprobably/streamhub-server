package com.streamhub.api;

import com.streamhub.nio.NIOServer;

/**
 * Provides the core publish and subscribe 
 * listening capabilities of the server.  The {@link SubscriptionManager} 
 * is used to add listeners for when a client publishes or 
 * subscribes.
 * <p>
 * To retrieve the {@link SubscriptionManager} use {@link NIOServer#getSubscriptionManager()}
 */
public interface SubscriptionManager {
	/**
	 * Adds a {@link SubscriptionListener} which will be notified 
	 * every time a client subscribes to a topic
	 * <p>
	 * A client may choose to subscribe to the same topic multiple times 
	 * in which case this method will be called multiple times
	 * 
	 * @param subscriptionListener	the listener to be notified of subscriptions
	 */
	void addSubscriptionListener(SubscriptionListener subscriptionListener);
	
	/**
	 * Removes a {@link SubscriptionListener} from being notified of subscription 
	 * events
	 * 
	 * @param subscriptionListener	the listener to be removed
	 */
	void removeSubscriptionListener(SubscriptionListener subscriptionListener);
	
	/**
	 * Adds a {@link PublishListener} which will be notified 
	 * every time a client publishes some data
	 * 
	 * @param publishListener	the listener to be notified of each publish
	 */
	void addPublishListener(PublishListener publishListener);
	
	/**
	 * Removes a {@link PublishListener} from being notified of  
	 * publish events
	 * 
	 * @param publishListener	the listener to be removed
	 */
	void removePublishListener(PublishListener publishListener);
}