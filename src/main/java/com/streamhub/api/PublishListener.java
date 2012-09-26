package com.streamhub.api;

import com.streamhub.nio.NIOServer;

/**
 * This interface is used to receive messages published by clients. 
 * Add your class as a {@link PublishListener} by using 
 * {@link SubscriptionManager#addPublishListener(PublishListener)}.  It is 
 * possible to get the {@link SubscriptionManager} using 
 * {@link NIOServer#getSubscriptionManager()}.
 *
 */
public interface PublishListener {
	/**
	 * This method will be called everytime a message is published 
	 * by a client to the server.
	 * 
	 * @param client	The client who published the message
	 * @param topic		The topic the message was published on
	 * @param payload	The data that was published
	 * 
	 * @see Client
	 * @see Payload
	 */
	void onMessageReceived(Client client, String topic, Payload payload);
}
