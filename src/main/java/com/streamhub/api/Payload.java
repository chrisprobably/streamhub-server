package com.streamhub.api;

import java.util.Map;

import com.streamhub.nio.NIOServer;

/**
 * A Payload represents a message which is to be 
 * sent over the wire to a client or has been received from a client.
 * A Payload must implement {@link #toCometBytes()}, {@link #toString()} 
 * and have a client capable of decoding it.
 * <p>
 * The recommended implementation of Payload is JsonPayload which 
 * is supported out of the box in all StreamHub clients.
 * 
 * @see JsonPayload
 */
public interface Payload {
	/**
	 * Adds a field to the message with the key and value.
	 * 
	 * @param key	the key of the message - must be unique
	 * @param value	the value of the message
	 */
	void addField(String key, String value);
	
	/**
	 * Returns a map of every single field added to this message
	 * 
	 * @return	A map of all the fields in this payload
	 */
	Map<String, String> getFields();
	
	/**
	 * Returns a byte array representing the bytes ready to be sent 
	 * over the wire to a Comet client.
	 * <p>
	 * This method should not be used to send a payload, 
	 * use {@link NIOServer#publish(String, Payload)} or 
	 * {@link Client#send(String, Payload)} instead.
	 * 
	 * @return	A byte array representing the bytes ready to be sent 
	 * 			over the wire to a Comet client
	 */
	byte[] toCometBytes();
	
	/**
	 * Toggles timestamping on or off.  By default timestamping is off.
	 * 
	 * @param onOrOff	A boolean representing whether to turn on timestamping 
	 * 					or not. Use <code>true</code> to enable it, and 
	 * 					<code>false</code> to disable it.
	 * @see #timestamp()
	 */
	void toggleTimestamping(boolean onOrOff); 
	
	/**
	 * If timestamping is enabled, adds a field named 'timestamp' to 
	 * the Payload with a value of {@link System#currentTimeMillis()}.
	 * 
	 * @see #toggleTimestamping(boolean)
	 */
	void timestamp();
}
