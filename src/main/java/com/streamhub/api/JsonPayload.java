package com.streamhub.api;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * JsonPayload sends its message 
 * as a JSON string.  Every message has at least one field 'topic' 
 * representing the topic that this message concerns.
 * <p>
 * Use the following to access the topic of a <code>JsonPayload</code>:
<pre>
Payload payload = new JsonPayload("GOOG");
String topic = payload.getFields().get("topic");
</pre>
 */
public class JsonPayload implements Payload {
	private static final Logger log = Logger.getLogger(JsonPayload.class);
	private JSONObject json = new JSONObject();
	private Map<String, String> fields = new HashMap<String, String>();
	private byte[] bytes;
	private boolean isTimestampingEnabled;
	private String jsonAsString;

	/**
	 * Creates a new JsonPayload with topic.  This adds a field 
	 * to the JSON message on the wire with a key of 'topic' and 
	 * a value of the parameter topic
	 * 
	 * @param topic	the topic this payload will be sent with
	 */
	public JsonPayload(String topic) {
		try {
			fields.put("topic", topic);
			json.put("topic", topic);
			jsonAsString = json.toString();
			bytes = ("<script>x(" + jsonAsString + ");</script>").getBytes();
		} catch (Exception e) {
			log.error("Error creating JSONObject", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.streamhub.api.Payload#addField(java.lang.String, java.lang.String)
	 */
	public void addField(String key, String value) {
		try {
			fields.put(key, value);
			json.put(key, value);
			jsonAsString = json.toString();
			bytes = ("<script>x(" + jsonAsString + ");</script>").getBytes();
		} catch (Exception e) {
			log.error("Error adding field key '" + key + "', value '" + value + "'", e);
		}
	}

	
	/* (non-Javadoc)
	 * @see com.streamhub.api.Payload#getFields()
	 */
	public Map<String, String> getFields() {
		return fields;
	}

	/* (non-Javadoc)
	 * @see com.streamhub.api.Payload#toCometBytes()
	 */
	public byte[] toCometBytes() {
		return bytes;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.streamhub.api.Payload#timestamp()
	 */
	public void timestamp() {
		if (isTimestampingEnabled) {
			addField("timestamp", String.valueOf(System.currentTimeMillis()));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.streamhub.api.Payload#toggleTimestamping(boolean)
	 */
	public void toggleTimestamping(boolean onOrOff) {
		isTimestampingEnabled = onOrOff;
	}

	public String toString() {
		return jsonAsString;
	}
}

