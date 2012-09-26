package com.streamhub.api;

import java.util.Set;

import com.streamhub.nio.NIOServer;

/**
 * Represents a client - either a browser (Comet) client or a direct client 
 * using one of the thick client SDKs (e.g. .NET or Java).  
 */
public interface Client {
	/**
	 * Sends a message to this client on a particular topic
	 * <p>
	 * Note the client does not need to be subscribed to topic but 
	 * they may choose to ignore the message if they are not.  To send a 
	 * message to all subscribers of a topic use {@link NIOServer#publish(String, Payload)}
	 * 
	 * @param topic		the topic to send the message with
	 * @param payload	the contents of the message
	 * 
	 * @see JsonPayload
	 * @see Payload
	 */
	void send(String topic, Payload payload);
	
	/**
	 * Disconnects this client.  The connection will be closed immediately.  
	 * The subscriptions will be removed after a six minute interval allowing 
	 * time for the client to reconnect.
	 */
	void disconnect();
	
	/**
	 * Whether this client is currently connected and capable 
	 * of receiving messages
	 * 
	 * @return <code>true</code> if the client is currently connected, otherwise <code>false</code>
	 */
	boolean isConnected();
	
	/**
	 * Returns a unique ID for each client
	 * 
	 * @return	a unique ID for this client
	 */
	String getUid();
	
	/**
	 * Returns a list of every topic the client is currently subscribed to
	 * 
	 * @return	the clients current subscriptions
	 */
	Set<String> getSubscriptions();
}
