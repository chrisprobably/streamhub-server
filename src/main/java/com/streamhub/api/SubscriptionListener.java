package com.streamhub.api;

import com.streamhub.nio.NIOServer;

/**
 * This interface is used to receive callbacks when a client 
 * subscribes or unsubscribes from a topic.
 * <p>
 * A subscription to a topic is a request to receive streaming 
 * data on this topic.  For example a subscription to the topic 
 * 'MSFT' could represent a request to receive stock quotes on 
 * the ticker symbol 'MSFT'. 
 */
public interface SubscriptionListener {
	/**
	 * Called when a client subscribes to a topic.  
	 * <p>
	 * It is possible 
	 * to return a direct response to the client using 
	 * {@link Client#send(String, Payload)}.  This may be used to 
	 * return some static data to a client.  However, after returning 
	 * any static data it is preferable to use {@link NIOServer#publish(String, Payload)} 
	 * to publish data to every client subscribed to a topic.
	 * 
	 * @param topic		The topic the client subscribed to
	 * @param client	The client who initiated the subscription
	 */
	void onSubscribe(String topic, Client client);
	
	/**
	 * Called when a client unsubscribes from a topic.  
	 * <p>
	 * It is possible to return a response to the client using 
	 * {@link Client#send(String, Payload)}.  However, by the time this 
	 * method has been called, the client will have been removed from 
	 * StreamHubs internal subscription map so any calls to 
	 * {@link NIOServer#publish(String, Payload)} will not send data to 
	 * this client.  If for some reason you wish to 
	 * still send data to this unsubscribed 
	 * client you will need to save the client parameter for later use.
	 * 
	 * @param topic		The topic the client unsubscribed from
	 * @param client	The client who unsubscribed
	 */	
	void onUnSubscribe(String topic, Client client);
}
