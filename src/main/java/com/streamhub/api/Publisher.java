package com.streamhub.api;

import com.streamhub.api.Payload;
import com.streamhub.nio.NIOServer;

/**
 * A Publisher allows you to publish to all subscribed clients on a particular topic.  
 * Only clients who are connected and subscribed to topic will receive the payload.
 */
public interface Publisher {
	/**
	 * Sends the payload to all clients who are subscribed to 
	 * topic.  Only connected clients who are subscribed to topic will 
	 * receive the payload.  This interface is implemented by {@link NIOServer}.
	 * <p>
	 * This method should generally be preferred over {@link Client#send(String, Payload)} 
	 * except where unique information must be sent to each client.
	 * <p>
	 * StreamHub keeps its own internal map of subscriptions.  If you need 
	 * to send something on a topic which a client has not subscribed to, use 
	 * {@link Client#send(String, Payload)}.
	 * 
	 * @param topic		the topic on which to send the message
	 * @param payload	the message payload
	 * 
	 * @see	Payload
	 */
	void publish(String topic, Payload payload);
}
